import { Component, input, output } from '@angular/core';
import { MarketConfig, formatMoney } from '../../core/market';

@Component({
  selector: 'app-product-step',
  template: `
    <h2>Choose an account</h2>
    <p class="lead">The account types offered in {{ config().displayName }}.</p>
    <div class="choices" role="radiogroup" aria-label="Account type">
      @for (p of config().products; track p.id) {
        <button
          type="button"
          class="choice"
          role="radio"
          [attr.aria-checked]="selected() === p.id"
          [class.choice--on]="selected() === p.id"
          (click)="select.emit(p.id)"
        >
          <span class="choice__name">{{ p.name }}</span>
          <span class="choice__text">{{ p.summary }}</span>
          <span class="tags">
            @if (p.depositCap !== null) {
              <span class="tag">Deposit cap {{ money(p.depositCap) }} / year</span>
            } @else {
              <span class="tag tag--quiet">No deposit cap</span>
            }
            @if (p.onePerPerson) {
              <span class="tag">One per person</span>
            }
          </span>
        </button>
      }
    </div>
  `,
})
export class ProductStep {
  readonly config = input.required<MarketConfig>();
  readonly selected = input<string | null>(null);
  readonly select = output<string>();

  protected money(amount: number): string {
    return formatMoney(amount, this.config());
  }
}
