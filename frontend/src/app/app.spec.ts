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
  const infoButton = (f: ComponentFixture<App>) =>
    page(f).querySelector<HTMLButtonElement>('.info-toggle')!;
  // Collapses all whitespace: Intl puts a non-breaking space between amount and currency.
  const panel = (f: ComponentFixture<App>) =>
    (page(f).querySelector('#demo-info')?.textContent ?? '').replace(/\s+/g, ' ');
  const facts = (f: ComponentFixture<App>) =>
    (page(f).querySelector('#demo-info dl')?.textContent ?? '').replace(/\s+/g, ' ');
  const button = (f: ComponentFixture<App>, name: string) =>
    [...page(f).querySelectorAll<HTMLButtonElement>('.switch button')].find((b) =>
      b.textContent?.includes(name),
    )!;

  async function openInfo(f: ComponentFixture<App>) {
    infoButton(f).click();
    await f.whenStable();
  }

  it('opens the first market', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    expect(page(fixture).textContent).toContain('Open an account · Sweden');
    expect(button(fixture, 'Sweden').getAttribute('aria-pressed')).toBe('true');
    expect(button(fixture, 'Denmark').getAttribute('aria-pressed')).toBe('false');
  });

  /** The page is the product. Notes about the demo only appear when a visitor asks for them. */
  it('keeps the demo notes closed until asked, and Esc closes them', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();

    expect(page(fixture).querySelector('#demo-info')).toBeNull();
    expect(infoButton(fixture).getAttribute('aria-expanded')).toBe('false');

    await openInfo(fixture);
    expect(infoButton(fixture).getAttribute('aria-expanded')).toBe('true');
    expect(document.activeElement?.id).toBe('demo-info-title');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await fixture.whenStable();
    expect(page(fixture).querySelector('#demo-info')).toBeNull();
    expect(document.activeElement).toBe(infoButton(fixture));
  });

  it('closes the notes from their own close button', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();
    await openInfo(fixture);

    page(fixture)
      .querySelector<HTMLButtonElement>('#demo-info button[aria-label="Close"]')!
      .click();
    await fixture.whenStable();

    expect(page(fixture).querySelector('#demo-info')).toBeNull();
    expect(infoButton(fixture).getAttribute('aria-expanded')).toBe('false');
  });

  it('shows what the market plugs in, and says what is shared only once', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();
    await openInfo(fixture);

    expect(panel(fixture)).toContain('Sweden and Denmark run the same code.');
    expect(panel(fixture)).toContain('What Sweden plugs in');
    expect(facts(fixture)).toContain('BankID');
    expect(facts(fixture)).not.toContain('+1 confirmation'); // Sweden has none
    expect(facts(fixture)).not.toContain('minimum age');
    expect(panel(fixture)).toContain('Same in every market');
    expect(panel(fixture)).toContain('nothing is stored');

    const repo = page(fixture).querySelector<HTMLAnchorElement>('#demo-info a')!;
    expect(repo.textContent).toContain('Source on GitHub');
    expect(repo.rel).toBe('noopener');
  });

  it('opens the market named in the link, e.g. #dk', async () => {
    history.replaceState(null, '', '#dk');
    const fixture = start();
    http.expectOne('/api/markets/DK/onboarding').flush(DK);
    await fixture.whenStable();
    await openInfo(fixture);

    expect(panel(fixture)).toContain('What Denmark plugs in');
    expect(facts(fixture)).toContain('cap 174.200 kr.');
    expect(facts(fixture)).toContain('+1 confirmation');
  });

  it('switches market on click, and the open notes follow', async () => {
    const fixture = start();
    http.expectOne('/api/markets/SE/onboarding').flush(SE);
    await fixture.whenStable();
    await openInfo(fixture);

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
    await openInfo(fixture);

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
