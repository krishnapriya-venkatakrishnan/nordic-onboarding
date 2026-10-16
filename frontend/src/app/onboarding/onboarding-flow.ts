import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  AsyncValidatorFn,
  FormControl,
  FormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Observable, Subscription, catchError, map, of, switchMap, tap, timer } from 'rxjs';
import { MarketApi } from '../core/market-api';
import {
  ApplicationResult,
  FieldError,
  MarketConfig,
  STEP_TITLES,
  StepType,
  disclosuresFor,
  stepForField,
} from '../core/market';
import { DetailsStep } from './steps/details-step';
import { DisclosuresStep } from './steps/disclosures-step';
import { IdentityStep } from './steps/identity-step';
import { ProductStep } from './steps/product-step';

/**
 * Renders whatever steps the market config lists, in that order. Holds the
 * application state; the step components only display and emit.
 */
@Component({
  selector: 'app-onboarding-flow',
  imports: [IdentityStep, DetailsStep, ProductStep, DisclosuresStep],
  templateUrl: './onboarding-flow.html',
})
export class OnboardingFlow {
  readonly config = input.required<MarketConfig>();

  private readonly api = inject(MarketApi);
  private readonly host: ElementRef<HTMLElement> = inject(ElementRef);
  private readonly injector = inject(Injector);

  protected readonly titles = STEP_TITLES;
  protected readonly stepIndex = signal(0);
  protected readonly step = computed<StepType>(() => this.config().steps[this.stepIndex()]);

  protected readonly sessionId = signal<string | null>(null);
  protected readonly verifying = signal(false);
  protected readonly verifyFailed = signal(false);
  private verification?: Subscription;
  protected readonly productId = signal<string | null>(null);
  protected readonly accepted = signal<ReadonlySet<string>>(new Set());
  protected readonly submitting = signal(false);
  private submission?: Subscription;
  protected readonly result = signal<ApplicationResult | null>(null);
  protected readonly serverErrors = signal<FieldError[]>([]);

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

  protected readonly visibleDisclosures = computed(() =>
    disclosuresFor(this.config(), this.productId()),
  );
  protected readonly chosenProduct = computed(() =>
    this.config().products.find((p) => p.id === this.productId()),
  );

  protected readonly canContinue = computed(() => {
    switch (this.step()) {
      case 'IDENTITY':
        return this.sessionId() !== null;
      case 'PERSONAL_DETAILS':
        return this.detailsStatus() === 'VALID';
      case 'ACCOUNT_TYPE':
        return this.productId() !== null;
      case 'DISCLOSURES':
        return this.visibleDisclosures()
          .filter((d) => d.required)
          .every((d) => this.accepted().has(d.id));
      case 'REVIEW':
        return !this.submitting();
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
    this.verifyFailed.set(false);
    this.verification = this.api.startIdentitySession(this.config().code).subscribe({
      next: (id) => {
        this.sessionId.set(id);
        this.verifying.set(false);
      },
      error: () => {
        this.verifying.set(false);
        this.verifyFailed.set(true);
      },
    });
  }

  protected selectProduct(id: string): void {
    this.productId.set(id);
    // Drop acceptances that belonged to the previously chosen product.
    const stillVisible = new Set(disclosuresFor(this.config(), id).map((d) => d.id));
    this.accepted.update((a) => new Set([...a].filter((x) => stillVisible.has(x))));
  }

  protected toggle(id: string): void {
    this.accepted.update((a) => {
      const next = new Set(a);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  protected next(): void {
    if (this.step() === 'PERSONAL_DETAILS') this.details.markAllAsTouched();
    if (!this.canContinue()) return;
    if (this.step() === 'REVIEW') {
      this.submit();
    } else {
      this.stepIndex.update((i) => i + 1);
      this.focusHeading();
    }
  }

  protected back(): void {
    this.stepIndex.update((i) => Math.max(0, i - 1));
    this.focusHeading();
  }

  protected goTo(step: StepType): void {
    const i = this.config().steps.indexOf(step);
    if (i >= 0 && i < this.stepIndex()) {
      this.stepIndex.set(i);
      this.focusHeading();
    }
  }

  protected fixError(field: string): void {
    this.goTo(stepForField(field));
  }

  protected fieldLabel(field: string): string {
    if (field === 'nationalId') return this.config().nationalId.label;
    return this.titles[stepForField(field)];
  }

  protected startOver(): void {
    this.reset(this.config());
    this.focusHeading();
  }

  /**
   * Moves focus to the new step's heading once it is on screen. Without this, focus stays on
   * a button that may have just disappeared, and screen reader users don't hear the new step.
   */
  private focusHeading(): void {
    afterNextRender(
      () => {
        const heading = this.host.nativeElement.querySelector<HTMLElement>('.card h2');
        heading?.setAttribute('tabindex', '-1');
        heading?.focus();
      },
      { injector: this.injector },
    );
  }

  private submit(): void {
    this.submitting.set(true);
    this.serverErrors.set([]);
    const v = this.details.getRawValue();
    this.submission = this.api
      .submit(this.config().code, {
        identitySessionId: this.sessionId(),
        nationalId: v.nationalId,
        fullName: v.fullName,
        email: v.email,
        productId: this.productId(),
        acceptedDisclosures: [...this.accepted()],
      })
      .subscribe({
        next: (r) => {
          this.submitting.set(false);
          if (r.accepted) {
            this.result.set(r);
            this.focusHeading();
            return;
          }
          // The server no longer accepts this eID session, so offer a fresh login.
          if (r.errors.some((e) => e.field === 'identity')) this.sessionId.set(null);
          this.serverErrors.set(r.errors);
        },
        error: () => {
          this.submitting.set(false);
          this.serverErrors.set([
            { field: 'form', message: 'Something went wrong. Please try again.' },
          ]);
        },
      });
  }

  private reset(config: MarketConfig): void {
    // Requests still in flight belong to the previous market; drop them.
    this.verification?.unsubscribe();
    this.submission?.unsubscribe();
    this.verifying.set(false);
    this.verifyFailed.set(false);
    this.submitting.set(false);
    this.stepIndex.set(0);
    this.sessionId.set(null);
    this.productId.set(null);
    this.accepted.set(new Set());
    this.result.set(null);
    this.serverErrors.set([]);
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
    // Coming back to the details step re-runs validation. Answering from memory keeps the
    // verdict on screen (instead of a "Checking…" nothing would clear) and skips a server call.
    let last: { value: string; errors: ValidationErrors | null } | null = null;
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      const value: string = control.value;
      if (!value) return of(null);
      if (last?.value === value) return of(last.errors);
      return timer(350).pipe(
        switchMap(() => this.api.checkNationalId(market, value)),
        map((msg) => (msg ? { server: msg } : null)),
        tap((errors) => (last = { value, errors })),
        // Without this, a network error would leave the field stuck on "Checking…". Not
        // remembered, so the next attempt asks the server again.
        catchError(() => of({ server: "Couldn't check the number right now. Try again." })),
      );
    };
  }
}
