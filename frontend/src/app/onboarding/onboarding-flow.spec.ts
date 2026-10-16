import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MarketConfig } from '../core/market';
import { MarketApi } from '../core/market-api';
import { DK, SE } from '../core/test-configs';
import { OnboardingFlow } from './onboarding-flow';

describe('OnboardingFlow: identity and details', () => {
  let fixture: ComponentFixture<OnboardingFlow>;
  let api: {
    startIdentitySession: ReturnType<typeof vi.fn>;
    checkNationalId: ReturnType<typeof vi.fn>;
  };

  const el = () => fixture.nativeElement as HTMLElement;
  const text = () => (el().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string) =>
    [...el().querySelectorAll('button')].find((b) => b.textContent?.trim() === label)!;
  const input = (name: string) =>
    el().querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;

  beforeEach(async () => {
    // Fake timers so the 350 ms typing pause can be skipped instead of waited for.
    vi.useFakeTimers();
    api = {
      startIdentitySession: vi.fn(() => of('sim-session')),
      checkNationalId: vi.fn(() => of(null)),
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
    button(`Continue with ${config.identityProvider.displayName}`).click();
    fixture.detectChanges();
    button('Continue').click();
    fixture.detectChanges();
  }

  it('unlocks Continue once the eID login succeeds', () => {
    show(DK);
    expect(button('Continue').disabled).toBe(true);

    button('Continue with MitID').click();
    fixture.detectChanges();

    expect(text()).toContain('Verified with MitID');
    expect(button('Continue').disabled).toBe(false);
  });

  it('drops a login that finishes after the market was switched', () => {
    const late = new Subject<string>();
    api.startIdentitySession.mockReturnValue(late);
    show(DK);
    button('Continue with MitID').click();
    fixture.detectChanges();

    show(SE);
    late.next('sim-mitid-late');
    fixture.detectChanges();

    expect(late.observed).toBe(false);
    expect(button('Continue with BankID').disabled).toBe(false);
    expect(button('Continue').disabled).toBe(true);
  });

  it('checks the ID number with the server once typing pauses, and only when the format fits', async () => {
    api.checkNationalId.mockReturnValue(of('You must be at least 18 to open an account yourself.'));
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
