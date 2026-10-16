import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ApplicationRequest, ApplicationResult } from './market';
import { MarketApi } from './market-api';

const REQUEST: ApplicationRequest = {
  identitySessionId: 'sim-mitid-1',
  nationalId: '121285-1234',
  fullName: 'Freja Jensen',
  email: 'freja@example.com',
  productId: 'depot',
  acceptedDisclosures: ['terms'],
};

describe('MarketApi', () => {
  let api: MarketApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(MarketApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('turns a 422 into a normal result carrying the field errors', async () => {
    const rejected: ApplicationResult = {
      accepted: false,
      applicationId: null,
      marketCode: 'DK',
      productId: null,
      currency: null,
      errors: [{ field: 'fullName', message: 'Enter your full name.' }],
    };
    const result = firstValueFrom(api.submit('DK', REQUEST));
    http
      .expectOne('/api/markets/DK/applications')
      .flush(rejected, { status: 422, statusText: 'Unprocessable Content' });

    expect(await result).toEqual(rejected);
  });

  it('still fails on other errors, so an outage is never shown as a form problem', async () => {
    const result = firstValueFrom(api.submit('DK', REQUEST));
    http
      .expectOne('/api/markets/DK/applications')
      .flush(null, { status: 500, statusText: 'Error' });

    await expect(result).rejects.toMatchObject({ status: 500 });
  });

  it('gives null for a valid ID and the server message otherwise', async () => {
    const valid = firstValueFrom(api.checkNationalId('DK', '121285-1234'));
    const check = http.expectOne('/api/markets/DK/national-id/check');
    expect(check.request.body).toEqual({ value: '121285-1234' }); // in the body, never the URL
    check.flush({ valid: true });
    expect(await valid).toBeNull();

    const tooYoung = firstValueFrom(api.checkNationalId('DK', '010115-4123'));
    http
      .expectOne('/api/markets/DK/national-id/check')
      .flush({ valid: false, message: 'You must be at least 18 to open an account yourself.' });
    expect(await tooYoung).toBe('You must be at least 18 to open an account yourself.');
  });
});
