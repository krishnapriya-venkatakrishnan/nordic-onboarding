import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MarketConfig } from '../../core/market';

type IdMessage = 'checking' | 'format' | 'server' | 'ok' | 'example';

/**
 * Each message sits outside its label (so it is not read as part of the field's name)
 * and is linked with aria-describedby. Messages are live regions: their text changes
 * in place, so screen readers hear each result of the ID check as it arrives.
 */
@Component({
  selector: 'app-details-step',
  imports: [ReactiveFormsModule],
  template: `
    <h2>Your details</h2>
    <p class="lead">
      You need to be at least {{ config().minimumAge }} to open an account yourself.
    </p>
    <form [formGroup]="form()" class="fields" (submit)="$event.preventDefault()">
      @let id = idMessage();
      <div class="field">
        <label for="nationalId">{{ config().nationalId.label }}</label>
        <input
          id="nationalId"
          formControlName="nationalId"
          [placeholder]="config().nationalId.placeholder"
          inputmode="numeric"
          autocomplete="off"
          aria-required="true"
          aria-describedby="nationalId-msg"
          [attr.aria-invalid]="id === 'format' || id === 'server'"
        />
        <small
          id="nationalId-msg"
          aria-live="polite"
          [class.hint]="id === 'checking' || id === 'example'"
          [class.hint--ok]="id === 'ok'"
          [class.error]="id === 'format' || id === 'server'"
        >
          @switch (id) {
            @case ('checking') {
              Checking…
            }
            @case ('format') {
              Use the format {{ config().nationalId.placeholder }}.
            }
            @case ('server') {
              {{ serverMessage() }}
            }
            @case ('ok') {
              Looks good.
            }
            @default {
              Demo test number: <code>{{ config().nationalId.example }}</code>
            }
          }
        </small>
      </div>
      <div class="field">
        <label for="fullName">Full name</label>
        <input
          id="fullName"
          formControlName="fullName"
          autocomplete="name"
          aria-required="true"
          aria-describedby="fullName-msg"
          [attr.aria-invalid]="showError('fullName')"
        />
        <small id="fullName-msg" class="error" aria-live="polite">
          @if (showError('fullName')) {
            Enter your full name.
          }
        </small>
      </div>
      <div class="field">
        <label for="email">Email</label>
        <input
          id="email"
          formControlName="email"
          type="email"
          autocomplete="email"
          aria-required="true"
          aria-describedby="email-msg"
          [attr.aria-invalid]="showError('email')"
        />
        <small id="email-msg" class="error" aria-live="polite">
          @if (showError('email')) {
            Enter a valid email address.
          }
        </small>
      </div>
    </form>
  `,
})
export class DetailsStep {
  readonly form = input.required<FormGroup>();
  readonly config = input.required<MarketConfig>();

  /** The format error waits until the field is left; the server's verdict shows as soon as it answers. */
  protected idMessage(): IdMessage {
    const id = this.form().controls['nationalId'];
    if (id.pending) return 'checking';
    if (id.touched && id.errors?.['pattern']) return 'format';
    if (id.errors?.['server']) return 'server';
    if (id.valid) return 'ok';
    return 'example';
  }

  protected serverMessage(): string {
    return this.form().controls['nationalId'].errors?.['server'] ?? '';
  }

  protected showError(name: string): boolean {
    const control = this.form().controls[name];
    return control.touched && control.invalid;
  }
}
