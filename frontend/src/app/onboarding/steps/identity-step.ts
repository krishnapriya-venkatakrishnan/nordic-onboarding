import { Component, input, output } from '@angular/core';

/** The market's national eID (BankID, MitID, ...). Simulated: no real login happens. */
@Component({
  selector: 'app-identity-step',
  template: `
    <h2>Verify your identity</h2>
    <p class="lead">
      We use {{ provider() }} to confirm who you are. This demo simulates the login.
    </p>
    @if (verified()) {
      <div class="notice notice--ok" role="status">
        <span class="tick" aria-hidden="true">✓</span> Verified with {{ provider() }}
      </div>
    } @else {
      <button type="button" class="btn btn--eid" (click)="verify.emit()" [disabled]="busy()">
        {{ busy() ? 'Waiting for ' + provider() + '…' : 'Continue with ' + provider() }}
      </button>
    }
  `,
})
export class IdentityStep {
  readonly provider = input.required<string>();
  readonly verified = input(false);
  readonly busy = input(false);
  readonly verify = output<void>();
}
