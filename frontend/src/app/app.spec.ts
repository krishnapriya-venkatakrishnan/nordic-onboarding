import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { MarketSummary } from './core/market';
import { DK, SE } from './core/test-configs';

const MARKETS: MarketSummary[] = [
  { code: 'SE', displayName: 'Sweden', identityProvider: 'BankID' },
  { code: 'DK', displayName: 'Denmark', identityProvider: 'MitID' },
];

describe('App shell', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    history.replaceState(null, '', '/');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    history.replaceState(null, '', '/');
  });

  /** Creates the app and answers its first request, the list of live markets. */
  function start(): ComponentFixture<App> {
    const fixture = TestBed.createComponent(App);
    http.expectOne('/api/markets').flush(MARKETS);
    return fixture;
  }

  const page = (f: ComponentFixture<App>) => f.nativeElement as HTMLElement;
  // Collapses all whitespace: Intl puts a non-breaking space between amount and currency.
  const panel = (f: ComponentFixture<App>) =>
    (page(f).querySelector('aside')?.textContent ?? '').replace(/\s+/g, ' ');
  const button = (f: ComponentFixture<App>, name: string) =>
    [...page(f).querySelectorAll<HTMLButtonElement>('.switch button')].find((b) =>
      b.textContent?.includes(name),
    )!;

  it('opens the first market and shows what it plugs in', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    expect(panel(fixture)).toContain('What Sweden plugs in');
    expect(panel(fixture)).toContain('BankID');
    expect(button(fixture, 'Sweden').getAttribute('aria-pressed')).toBe('true');
    expect(button(fixture, 'Denmark').getAttribute('aria-pressed')).toBe('false');
  });

  it('opens the market named in the link, e.g. #dk', async () => {
    history.replaceState(null, '', '#dk');
    const fixture = start();
    http.expectOne('/api/markets/DK/onboarding').flush(DK);
    await fixture.whenStable();

    expect(panel(fixture)).toContain('What Denmark plugs in');
    expect(panel(fixture)).toContain('cap 174.200 kr.');
    expect(panel(fixture)).toContain('1 required · 1 product-specific');
  });

  it('switches market on click and updates the link', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    button(fixture, 'Denmark').click();
    http.expectOne('/api/markets/DK/onboarding').flush(DK);
    await fixture.whenStable();

    expect(panel(fixture)).toContain('What Denmark plugs in');
    expect(location.hash).toBe('#dk');
  });

  it('ends on the last market clicked, even if an older answer is still on its way', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    button(fixture, 'Denmark').click();
    const denmark = http.expectOne('/api/markets/DK/onboarding');
    button(fixture, 'Sweden').click();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    expect(denmark.cancelled).toBe(true);
    expect(panel(fixture)).toContain('What Sweden plugs in');
  });

  it('says so when the API cannot be reached', async () => {
    const fixture = TestBed.createComponent(App);
    http.expectOne('/api/markets').flush(null, { status: 504, statusText: 'Gateway Timeout' });
    await fixture.whenStable();

    expect(page(fixture).querySelector('[role="alert"]')?.textContent).toContain(
      "Couldn't reach the API",
    );
  });
});
