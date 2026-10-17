import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { MarketApi } from './core/market-api';
import { MarketConfig, MarketSummary, formatMoney } from './core/market';
import { OnboardingFlow } from './onboarding/onboarding-flow';

@Component({
  selector: 'app-root',
  imports: [OnboardingFlow],
  templateUrl: './app.html',
  styleUrl: './app.css',
  host: { '(document:keydown.escape)': 'closeInfo()' },
})
export class App {
  private readonly api = inject(MarketApi);
  private readonly choices = new Subject<string>();
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly infoButton = viewChild.required<ElementRef<HTMLButtonElement>>('infoButton');

  protected readonly markets = signal<MarketSummary[]>([]);
  /** The market last clicked. The config follows it once it has loaded. */
  protected readonly selected = signal<string | null>(null);
  protected readonly config = signal<MarketConfig | null>(null);
  protected readonly loadError = signal(false);

  /** The "How this demo works" panel: notes for visitors, not part of the customer's flow. */
  protected readonly infoOpen = signal(false);
  /** "Sweden and Denmark", from whichever markets are live, so the app names no country itself. */
  protected readonly marketNames = computed(() =>
    new Intl.ListFormat('en', { type: 'conjunction' }).format(
      this.markets().map((m) => m.displayName),
    ),
  );

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

  protected toggleInfo(): void {
    if (this.infoOpen()) {
      this.closeInfo();
      return;
    }
    this.infoOpen.set(true);
    // Move focus into the panel once it exists, so keyboard and screen reader users land there.
    afterNextRender(
      () => this.host.nativeElement.querySelector<HTMLElement>('#demo-info-title')?.focus(),
      { injector: this.injector },
    );
  }

  protected closeInfo(): void {
    if (!this.infoOpen()) return;
    // Focus inside the panel would be lost when it goes, so hand it back to the button.
    // Esc pressed while typing in the flow leaves focus where it is.
    const panel = this.host.nativeElement.querySelector('#demo-info');
    const focusWasInside = panel?.contains(document.activeElement) ?? false;
    this.infoOpen.set(false);
    if (focusWasInside) this.infoButton().nativeElement.focus();
  }

  protected money(amount: number, c: MarketConfig): string {
    return formatMoney(amount, c);
  }

  /** Confirmations that only this product asks for, e.g. Aktiesparekonto's two. */
  protected extraConfirmations(c: MarketConfig, productId: string): number {
    return c.disclosures.filter((d) => d.productId === productId).length;
  }
}
