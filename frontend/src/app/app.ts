import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { MarketApi } from './core/market-api';
import { MarketConfig, MarketSummary } from './core/market';
import { OnboardingFlow } from './onboarding/onboarding-flow';

@Component({
  selector: 'app-root',
  imports: [OnboardingFlow],
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
  /** Why no market can be shown: no answer from the server, or the market has been switched off. */
  protected readonly loadError = signal<'unreachable' | 'unavailable' | null>(null);

  constructor() {
    // switchMap drops a slower, older response, so quick switching always ends on the last click.
    this.choices
      .pipe(
        switchMap((code) =>
          this.api.config(code).pipe(
            catchError((e: HttpErrorResponse) => {
              // 404: the server answered, but the market was switched off after the page loaded.
              this.loadError.set(e.status === 404 ? 'unavailable' : 'unreachable');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((c) => {
        // Choosing another market after an error shows that market again.
        this.loadError.set(null);
        this.config.set(c);
      });

    this.api.markets().subscribe({
      next: (list) => {
        this.markets.set(list);
        // #dk opens Denmark directly, so a link can point at one market.
        const fromHash = location.hash.slice(1).toUpperCase();
        const start = list.find((m) => m.code === fromHash) ?? list[0];
        if (start) this.choose(start.code);
      },
      error: () => this.loadError.set('unreachable'),
    });
  }

  protected choose(code: string): void {
    if (this.selected() === code) return;
    history.replaceState(null, '', '#' + code.toLowerCase());
    this.selected.set(code);
    this.choices.next(code);
  }
}
