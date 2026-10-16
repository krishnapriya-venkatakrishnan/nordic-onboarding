import { Component, input, output } from '@angular/core';
import { Disclosure } from '../../core/market';

@Component({
  selector: 'app-disclosures-step',
  template: `
    <h2>Confirmations</h2>
    <p class="lead">Required items are marked. Some depend on the account you chose.</p>
    <ul class="checks">
      @for (d of disclosures(); track d.id) {
        <li>
          <label class="check">
            <input type="checkbox" [checked]="accepted().has(d.id)" (change)="toggle.emit(d.id)" />
            <span>
              {{ d.text }}
              @if (d.required) {
                <em class="req">Required</em>
              }
              @if (d.productId) {
                <em class="scoped">For this account</em>
              }
            </span>
          </label>
        </li>
      }
    </ul>
  `,
})
export class DisclosuresStep {
  readonly disclosures = input.required<Disclosure[]>();
  readonly accepted = input.required<ReadonlySet<string>>();
  readonly toggle = output<string>();
}
