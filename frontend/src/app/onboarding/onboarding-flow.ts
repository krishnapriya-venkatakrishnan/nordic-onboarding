import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  AsyncValidatorFn,
  FormControl,
  FormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Observable, Subscription, catchError, map, of, switchMap, timer } from 'rxjs';
import { MarketApi } from '../core/market-api';
import { MarketConfig, STEP_TITLES, StepType } from '../core/market';
import { DetailsStep } from './steps/details-step';
import { IdentityStep } from './steps/identity-step';

/**
 * Renders whatever steps the market config lists, in that order. Holds the
 * application state; the step components only display and emit.
 */
@Component({
  selector: 'app-onboarding-flow',
  imports: [IdentityStep, DetailsStep],
  templateUrl: './onboarding-flow.html',
})
export class OnboardingFlow {
  readonly config = input.required<MarketConfig>();

  private readonly api = inject(MarketApi);

  protected readonly titles = STEP_TITLES;
  protected readonly stepIndex = signal(0);
  protected readonly step = computed<StepType>(() => this.config().steps[this.stepIndex()]);

  protected readonly sessionId = signal<string | null>(null);
  protected readonly verifying = signal(false);
  private verification?: Subscription;

  protected readonly details = new FormGroup({
    nationalId: new FormControl('', { nonNullable: true }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
  });
  /** Mirrors the form status into a signal so step navigation reacts to async validation. */
  private readonly detailsStatus = signal(this.details.status);

  protected readonly canContinue = computed(() => {
    switch (this.step()) {
      case 'IDENTITY':
        return this.sessionId() !== null;
      case 'PERSONAL_DETAILS':
        return this.detailsStatus() === 'VALID';
      default:
        // The remaining steps are added together with their components.
        return false;
    }
  });

  constructor() {
    this.details.statusChanges
      .pipe(takeUntilDestroyed())
      .subscribe((s) => this.detailsStatus.set(s));

    // A new market means a new flow: reset everything and re-arm the ID validators.
    effect(() => {
      const config = this.config();
      untracked(() => this.reset(config));
    });
  }

  protected verify(): void {
    this.verifying.set(true);
    this.verification = this.api.startIdentitySession(this.config().code).subscribe({
      next: (id) => {
        this.sessionId.set(id);
        this.verifying.set(false);
      },
      error: () => this.verifying.set(false),
    });
  }

  protected next(): void {
    if (this.step() === 'PERSONAL_DETAILS') this.details.markAllAsTouched();
    if (!this.canContinue()) return;
    this.stepIndex.update((i) => i + 1);
  }

  protected back(): void {
    this.stepIndex.update((i) => Math.max(0, i - 1));
  }

  protected goTo(step: StepType): void {
    const i = this.config().steps.indexOf(step);
    if (i >= 0 && i < this.stepIndex()) this.stepIndex.set(i);
  }

  private reset(config: MarketConfig): void {
    // A login still in flight belongs to the previous market; drop it.
    this.verification?.unsubscribe();
    this.verifying.set(false);
    this.stepIndex.set(0);
    this.sessionId.set(null);
    this.details.reset();
    const id = this.details.controls.nationalId;
    id.setValidators([Validators.required, Validators.pattern(config.nationalId.pattern)]);
    id.setAsyncValidators(this.serverIdCheck(config.code));
    id.updateValueAndValidity({ emitEvent: false });
    this.detailsStatus.set(this.details.status);
  }

  /**
   * The server owns the real rule (checksum, real date, minimum age). Angular drops the
   * previous check on every keystroke, so the timer works as a debounce.
   */
  private serverIdCheck(market: string): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> =>
      control.value
        ? timer(350).pipe(
            switchMap(() => this.api.checkNationalId(market, control.value)),
            map((msg) => (msg ? { server: msg } : null)),
            // Without this, a network error would leave the field stuck on "Checking…".
            catchError(() => of({ server: "Couldn't check the number right now. Try again." })),
          )
        : of(null);
  }
}
