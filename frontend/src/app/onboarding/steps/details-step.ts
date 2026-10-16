import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MarketConfig } from '../../core/market';

@Component({
  selector: 'app-details-step',
  imports: [ReactiveFormsModule],
  template: `
    <h2>Your details</h2>
    <p class="lead">
      You need to be at least {{ config().minimumAge }} to open an account yourself.
    </p>
    <form [formGroup]="form()" class="fields" (submit)="$event.preventDefault()">
      <label class="field">
        <span>{{ config().nationalId.label }}</span>
        <input
          formControlName="nationalId"
          [placeholder]="config().nationalId.placeholder"
          inputmode="numeric"
          autocomplete="off"
        />
        @let id = form().controls['nationalId'];
        @if (id.pending) {
          <small class="hint">Checking…</small>
        } @else if (id.touched && id.errors?.['pattern']) {
          <small class="error">Use the format {{ config().nationalId.placeholder }}.</small>
        } @else if (id.errors?.['server']) {
          <!-- Shown as soon as the check answers: it only runs once the format is right. -->
          <small class="error">{{ id.errors?.['server'] }}</small>
        } @else if (id.valid) {
          <small class="hint hint--ok">Looks good.</small>
        } @else {
          <small class="hint">
            Demo test number: <code>{{ config().nationalId.example }}</code>
          </small>
        }
      </label>
      <label class="field">
        <span>Full name</span>
        <input formControlName="fullName" autocomplete="name" />
      </label>
      <label class="field">
        <span>Email</span>
        <input formControlName="email" type="email" autocomplete="email" />
        @if (form().controls['email'].touched && form().controls['email'].invalid) {
          <small class="error">Enter a valid email address.</small>
        }
      </label>
    </form>
  `,
})
export class DetailsStep {
  readonly form = input.required<FormGroup>();
  readonly config = input.required<MarketConfig>();
}
