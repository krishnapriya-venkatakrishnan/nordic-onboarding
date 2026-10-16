package dev.krishnapriya.onboarding.domain;

public class UnknownMarketException extends RuntimeException {
    public UnknownMarketException(String code) {
        super("Market not available: " + code);
    }
}
