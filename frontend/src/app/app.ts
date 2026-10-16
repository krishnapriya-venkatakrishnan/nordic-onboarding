import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { MarketApi } from './core/market-api';
import {
  MarketConfig,
  MarketSummary,
  STEP_TITLES,
  disclosuresFor,
  formatMoney,
} from './core/market';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly api = inject(MarketApi);
  private readonly choices = new Subject<string>();

  protected readonly markets = signal<MarketSummary[]>([]);
  /** The market last clicked. The config follows it once it has loaded. */
  protected readonly selected = signal<string | null>(null);
  protected readonly config = signal<MarketConfig | null>(null);
  protected readonly loadError = signal(false);
  protected readonly titles = STEP_TITLES;

  constructor() {
    // switchMap drops a slower, older response, so quick switching always ends on the last click.
    this.choices
      .pipe(
        switchMap((code) =>
          this.api.config(code).pipe(
            catchError(() => {
              this.loadError.set(true);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((c) => this.config.set(c));

    this.api.markets().subscribe({
      next: (list) => {
        this.markets.set(list);
        // #dk opens Denmark directly, so a link can point at one market.
        const fromHash = location.hash.slice(1).toUpperCase();
        const start = list.find((m) => m.code === fromHash) ?? list[0];
        if (start) this.choose(start.code);
      },
      error: () => this.loadError.set(true),
    });
  }

  protected choose(code: string): void {
    if (this.selected() === code) return;
    history.replaceState(null, '', '#' + code.toLowerCase());
    this.selected.set(code);
    this.choices.next(code);
  }

  protected money(amount: number, c: MarketConfig): string {
    return formatMoney(amount, c);
  }

  protected scopedCount(c: MarketConfig): number {
    return c.disclosures.filter((d) => d.productId !== null).length;
  }

  protected requiredCount(c: MarketConfig): number {
    return disclosuresFor(c, null).filter((d) => d.required).length;
  }
}
