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
  const heading = (f: ComponentFixture<App>) =>
    page(f).querySelector('.eyebrow')?.textContent?.trim();
  const button = (f: ComponentFixture<App>, name: string) =>
    [...page(f).querySelectorAll<HTMLButtonElement>('.switch button')].find((b) =>
      b.textContent?.includes(name),
    )!;

  it('opens the first market', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    expect(heading(fixture)).toBe('Open an account · Sweden');
    expect(button(fixture, 'Sweden').getAttribute('aria-pressed')).toBe('true');
    expect(button(fixture, 'Denmark').getAttribute('aria-pressed')).toBe('false');
  });

  it('opens the market named in the link, e.g. #dk', async () => {
    history.replaceState(null, '', '#dk');
    const fixture = start();
    http.expectOne('/api/markets/DK/onboarding').flush(DK);
    await fixture.whenStable();

    expect(heading(fixture)).toBe('Open an account · Denmark');
  });

  it('switches market on click and updates the link', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    button(fixture, 'Denmark').click();
    http.expectOne('/api/markets/DK/onboarding').flush(DK);
    await fixture.whenStable();

    expect(heading(fixture)).toBe('Open an account · Denmark');
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
    expect(heading(fixture)).toBe('Open an account · Sweden');
  });

  it('says on every page that this is a demo with illustrative rules', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    expect(page(fixture).querySelector('footer')?.textContent?.trim()).toBe(
      'Portfolio demo · not a bank · rules are illustrative',
    );
  });

  it('says when a market was switched off after the page loaded, and recovers on another', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    button(fixture, 'Denmark').click();
    http
      .expectOne('/api/markets/DK/onboarding')
      .flush({ detail: 'Market not available: DK' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    expect(page(fixture).querySelector('[role="alert"]')?.textContent?.trim()).toBe(
      "This market isn't available right now.",
    );

    button(fixture, 'Sweden').click();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();
    expect(page(fixture).querySelector('[role="alert"]')).toBeNull();
    expect(heading(fixture)).toBe('Open an account · Sweden');
  });

  it('says so when the API cannot be reached', async () => {
    const fixture = TestBed.createComponent(App);
    http.expectOne('/api/markets').flush(null, { status: 504, statusText: 'Gateway Timeout' });
    await fixture.whenStable();

    expect(page(fixture).querySelector('[role="alert"]')?.textContent).toContain(
      "Couldn't reach the server",
    );
  });
});
