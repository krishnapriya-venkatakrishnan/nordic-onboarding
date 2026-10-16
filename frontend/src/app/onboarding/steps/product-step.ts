import { Component, input, output } from '@angular/core';
import { MarketConfig, formatMoney } from '../../core/market';

/** Native radio buttons styled as cards: keyboard users move between accounts with the arrow keys. */
@Component({
  selector: 'app-product-step',
  template: `
    <h2>Choose an account</h2>
    <p class="lead">The account types offered in {{ config().displayName }}.</p>
    <fieldset class="choices">
      <legend class="sr-only">Account type</legend>
      @for (p of config().products; track p.id) {
        <label class="choice" [class.choice--on]="selected() === p.id">
          <input
            type="radio"
            class="choice__radio"
            name="product"
            [value]="p.id"
            [checked]="selected() === p.id"
            (change)="select.emit(p.id)"
          />
          <!-- Product names are Danish or Swedish words; the lang lets screen readers say them right. -->
          <span class="choice__name" [attr.lang]="config().locale">{{ p.name }}</span>
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
        </label>
      }
    </fieldset>
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
