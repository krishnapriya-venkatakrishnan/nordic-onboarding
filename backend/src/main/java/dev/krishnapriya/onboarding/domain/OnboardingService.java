package dev.krishnapriya.onboarding.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The market-agnostic onboarding flow.
 *
 * Nothing in here knows which countries exist. It asks the {@link Market} for rules
 * and applies the same policy everywhere (identity verified, valid ID, minimum age,
 * product offered in that market, required disclosures accepted).
 */
public class OnboardingService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MarketRegistry markets;
    private final Clock clock;
    /** Simulated eID sessions: session id -> market code. A real system would verify a signed assertion. */
    private final Map<String, String> identitySessions = new ConcurrentHashMap<>();

    public OnboardingService(MarketRegistry markets, Clock clock) {
        this.markets = markets;
        this.clock = clock;
    }

    public MarketConfig config(String marketCode) {
        Market market = markets.get(marketCode);
        int year = today().getYear();
        List<MarketConfig.Product> products = market.products().stream()
                .map(p -> new MarketConfig.Product(p.id(), p.name(), p.summary(),
                        p.depositCapFor(year).orElse(null), p.onePerPerson()))
                .toList();
        NationalIdRule id = market.nationalIdRule();
        return new MarketConfig(
                market.code(),
                market.displayName(),
                market.locale().toLanguageTag(),
                market.currency().getCurrencyCode(),
                market.identityProvider(),
                new MarketConfig.NationalIdField(id.label(), id.placeholder(), id.formatPattern(), id.example()),
                market.minimumAge(),
                market.steps(),
                products,
                market.disclosures());
    }

    public List<MarketConfig.Summary> available() {
        return markets.all().stream()
                .map(m -> new MarketConfig.Summary(m.code(), m.displayName(), m.identityProvider().displayName()))
                .toList();
    }

    /** Simulates a successful eID login. Returns a session id the final submit must present. */
    public String startIdentitySession(String marketCode) {
        Market market = markets.get(marketCode);
        String sessionId = "sim-" + market.identityProvider().id() + "-" + UUID.randomUUID();
        identitySessions.put(sessionId, market.code());
        return sessionId;
    }

    /** Validates a national ID and the age policy, for instant feedback in the UI. */
    public Optional<FieldError> checkNationalId(String marketCode, String raw) {
        return checkNationalId(markets.get(marketCode), raw);
    }

    public ApplicationResult submit(String marketCode, ApplicationRequest request) {
        Market market = markets.get(marketCode);
        List<FieldError> errors = new ArrayList<>();

        String session = request.identitySessionId();
        if (session == null || !market.code().equals(identitySessions.get(session))) {
            errors.add(new FieldError("identity",
                    "Verify your identity with " + market.identityProvider().displayName() + " first."));
        }

        checkNationalId(market, request.nationalId()).ifPresent(errors::add);

        if (isBlank(request.fullName())) {
            errors.add(new FieldError("fullName", "Enter your full name."));
        }
        if (isBlank(request.email()) || !EMAIL.matcher(request.email().trim()).matches()) {
            errors.add(new FieldError("email", "Enter a valid email address."));
        }

        boolean productOffered = market.products().stream().anyMatch(p -> p.id().equals(request.productId()));
        if (!productOffered) {
            errors.add(new FieldError("productId", "Choose an account type offered in " + market.displayName() + "."));
        }

        market.disclosures().stream()
                .filter(Disclosure::required)
                .filter(d -> d.appliesTo(request.productId()))
                .filter(d -> !request.acceptedDisclosures().contains(d.id()))
                .forEach(d -> errors.add(new FieldError("disclosures." + d.id(), "This must be accepted to continue.")));

        if (!errors.isEmpty()) {
            return ApplicationResult.rejected(market.code(), errors);
        }
        identitySessions.remove(session);
        return ApplicationResult.accepted("APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                market.code(), request.productId(), market.currency().getCurrencyCode());
    }

    private Optional<FieldError> checkNationalId(Market market, String raw) {
        // Read the clock once, so parsing and the age check can't see two different dates around midnight.
        LocalDate today = today();
        NationalIdRule rule = market.nationalIdRule();
        NationalIdResult result = rule.parse(raw == null ? "" : raw, today);
        if (!result.valid()) {
            return Optional.of(new FieldError("nationalId", result.error()));
        }
        int age = Period.between(result.birthDate(), today).getYears();
        if (age < market.minimumAge()) {
            return Optional.of(new FieldError("nationalId",
                    "You must be at least " + market.minimumAge() + " to open an account yourself."));
        }
        return Optional.empty();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
