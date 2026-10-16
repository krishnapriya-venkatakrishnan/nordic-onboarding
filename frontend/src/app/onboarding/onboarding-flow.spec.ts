import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApplicationResult, MarketConfig } from '../core/market';
import { MarketApi } from '../core/market-api';
import { DK, SE } from '../core/test-configs';
import { OnboardingFlow } from './onboarding-flow';

describe('OnboardingFlow', () => {
  let fixture: ComponentFixture<OnboardingFlow>;
  let api: {
    startIdentitySession: ReturnType<typeof vi.fn>;
    checkNationalId: ReturnType<typeof vi.fn>;
    submit: ReturnType<typeof vi.fn>;
  };

  const el = () => fixture.nativeElement as HTMLElement;
  const text = () => (el().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string) =>
    [...el().querySelectorAll('button')].find((b) => b.textContent?.trim() === label)!;
  const input = (name: string) =>
    el().querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
  const choice = (name: string) =>
    [...el().querySelectorAll<HTMLButtonElement>('.choice')].find((c) =>
      c.textContent?.includes(name),
    )!;
  const checkbox = (text: string) =>
    [...el().querySelectorAll('label.check')]
      .find((l) => l.textContent?.includes(text))!
      .querySelector('input')!;

  beforeEach(async () => {
    // Fake timers so the 350 ms typing pause can be skipped instead of waited for.
    vi.useFakeTimers();
    api = {
      startIdentitySession: vi.fn(() => of('sim-session')),
      checkNationalId: vi.fn(() => of(null)),
      submit: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [OnboardingFlow],
      providers: [{ provide: MarketApi, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(OnboardingFlow);
  });

  afterEach(() => vi.useRealTimers());

  function show(config: MarketConfig) {
    fixture.componentRef.setInput('config', config);
    fixture.detectChanges();
  }

  function click(target: HTMLElement) {
    target.click();
    fixture.detectChanges();
  }

  function type(name: string, value: string) {
    input(name).value = value;
    input(name).dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  async function pause() {
    await vi.advanceTimersByTimeAsync(350);
    fixture.detectChanges();
  }

  /** Verifies with the eID and moves on to the details step. */
  function toDetails(config: MarketConfig) {
    show(config);
    click(button(`Continue with ${config.identityProvider.displayName}`));
    click(button('Continue'));
  }

  /** Fills valid details and moves on to the account type step. */
  async function toAccountType(config: MarketConfig) {
    toDetails(config);
    type('nationalId', config.nationalId.example);
    type('fullName', 'Freja Jensen');
    type('email', 'freja@example.com');
    await pause();
    click(button('Continue'));
  }

  /** Takes Denmark's Depot through to the review step. */
  async function toReview() {
    await toAccountType(DK);
    click(choice('Depot'));
    click(button('Continue'));
    click(checkbox('Accept terms'));
    click(button('Continue'));
  }

  const rejected = (...errors: ApplicationResult['errors']): ApplicationResult => ({
    accepted: false,
    applicationId: null,
    marketCode: 'DK',
    productId: null,
    currency: null,
    errors,
  });

  describe('identity and details', () => {
    it('unlocks Continue once the eID login succeeds', () => {
      show(DK);
      expect(button('Continue').disabled).toBe(true);

      click(button('Continue with MitID'));

      expect(text()).toContain('Verified with MitID');
      expect(button('Continue').disabled).toBe(false);
    });

    it('drops a login that finishes after the market was switched', () => {
      const late = new Subject<string>();
      api.startIdentitySession.mockReturnValue(late);
      show(DK);
      click(button('Continue with MitID'));

      show(SE);
      late.next('sim-mitid-late');
      fixture.detectChanges();

      expect(late.observed).toBe(false);
      expect(button('Continue with BankID').disabled).toBe(false);
      expect(button('Continue').disabled).toBe(true);
    });

    it('checks the ID number with the server once typing pauses, and only when the format fits', async () => {
      api.checkNationalId.mockReturnValue(
        of('You must be at least 18 to open an account yourself.'),
      );
      toDetails(DK);

      type('nationalId', '0101');
      await pause();
      expect(api.checkNationalId).not.toHaveBeenCalled();

      type('nationalId', '010115-412');
      type('nationalId', '010115-4123');
      expect(text()).toContain('Checking…');
      await pause();

      expect(api.checkNationalId).toHaveBeenCalledTimes(1);
      expect(api.checkNationalId).toHaveBeenCalledWith('DK', '010115-4123');
      expect(text()).toContain('You must be at least 18 to open an account yourself.');
    });

    it('says so when the ID check cannot reach the server', async () => {
      api.checkNationalId.mockReturnValue(throwError(() => new Error('offline')));
      toDetails(DK);

      type('nationalId', '121285-1234');
      await pause();

      expect(text()).toContain("Couldn't check the number right now.");
      expect(text()).not.toContain('Checking…');
    });

    it('unlocks Continue on the details step only when every field is valid', async () => {
      toDetails(SE);
      type('nationalId', '19121212-1212');
      await pause();
      expect(text()).toContain('Looks good.');

      type('fullName', 'Astrid Lund');
      type('email', 'astrid@');
      expect(button('Continue').disabled).toBe(true);

      type('email', 'astrid@example.com');
      expect(button('Continue').disabled).toBe(false);
    });
  });

  describe('account type, confirmations and review', () => {
    it("shows each account's deposit cap in the market's own money", async () => {
      await toAccountType(DK);

      expect(choice('Aktiesparekonto').textContent?.replace(/\s+/g, ' ')).toContain(
        'Deposit cap 174.200 kr. / year',
      );
      expect(choice('Depot').textContent).toContain('No deposit cap');
      expect(button('Continue').disabled).toBe(true);

      click(choice('Depot'));
      expect(choice('Depot').getAttribute('aria-checked')).toBe('true');
      expect(button('Continue').disabled).toBe(false);
    });

    it('asks for product-specific confirmations only for that product', async () => {
      await toAccountType(DK);
      click(choice('Aktiesparekonto'));
      click(button('Continue'));

      expect(text()).toContain('No other Aktiesparekonto');
      click(checkbox('Accept terms'));
      expect(button('Continue').disabled).toBe(true);
      click(checkbox('No other Aktiesparekonto'));
      expect(button('Continue').disabled).toBe(false);

      click(button('Back'));
      click(choice('Depot'));
      click(button('Continue'));
      expect(text()).not.toContain('No other Aktiesparekonto');
    });

    it('forgets a product-specific confirmation when the product changes', async () => {
      await toAccountType(DK);
      click(choice('Aktiesparekonto'));
      click(button('Continue'));
      click(checkbox('Accept terms'));
      click(checkbox('No other Aktiesparekonto'));

      click(button('Back'));
      click(choice('Depot'));
      click(choice('Aktiesparekonto'));
      click(button('Continue'));

      expect(checkbox('Accept terms').checked).toBe(true);
      expect(checkbox('No other Aktiesparekonto').checked).toBe(false);
    });

    it('submits the whole application and shows the reference', async () => {
      api.submit.mockReturnValue(
        of({
          accepted: true,
          applicationId: 'APP-1234ABCD',
          marketCode: 'DK',
          productId: 'depot',
          currency: 'DKK',
          errors: [],
        }),
      );
      await toReview();
      expect(text()).toContain('121285-1234');

      click(button('Open account'));

      expect(api.submit).toHaveBeenCalledWith('DK', {
        identitySessionId: 'sim-session',
        nationalId: '121285-1234',
        fullName: 'Freja Jensen',
        email: 'freja@example.com',
        productId: 'depot',
        acceptedDisclosures: ['terms'],
      });
      expect(text()).toContain('Demo application complete');
      expect(text()).toContain('No account was opened and nothing was saved.');
      expect(text()).toContain('APP-1234ABCD');

      click(button('Start a new application'));
      expect(button('Continue with MitID')).toBeTruthy();
    });

    it('links each server error back to the step that owns the field', async () => {
      api.submit.mockReturnValue(
        of(
          rejected(
            { field: 'fullName', message: 'Enter your full name.' },
            { field: 'disclosures.terms', message: 'This must be accepted to continue.' },
          ),
        ),
      );
      await toReview();
      click(button('Open account'));

      expect(text()).toContain('Enter your full name.');
      expect(button('Fix in Confirmations')).toBeTruthy();

      click(button('Fix in Your details'));
      expect(input('fullName')).toBeTruthy();
    });

    it('asks for a fresh eID login when the server rejects the session', async () => {
      api.submit.mockReturnValue(
        of(rejected({ field: 'identity', message: 'Verify your identity with MitID first.' })),
      );
      await toReview();
      click(button('Open account'));

      click(button('Fix in Verify identity'));

      expect(text()).not.toContain('Verified with MitID');
      expect(button('Continue with MitID').disabled).toBe(false);
    });

    it('drops a submission that finishes after the market was switched', async () => {
      const late = new Subject<ApplicationResult>();
      api.submit.mockReturnValue(late);
      await toReview();
      click(button('Open account'));
      expect(text()).toContain('Opening…');

      show(SE);
      late.next({ ...rejected(), accepted: true, applicationId: 'APP-LATE' });
      fixture.detectChanges();

      expect(late.observed).toBe(false);
      expect(text()).not.toContain('Demo application complete');
      expect(button('Continue with BankID')).toBeTruthy();
    });
  });
});
