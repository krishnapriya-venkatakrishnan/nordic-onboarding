import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { ApplicationRequest, ApplicationResult, MarketConfig, MarketSummary } from './market';

@Injectable({ providedIn: 'root' })
export class MarketApi {
  private readonly http = inject(HttpClient);

  markets(): Observable<MarketSummary[]> {
    return this.http.get<MarketSummary[]>('/api/markets');
  }

  config(code: string): Observable<MarketConfig> {
    return this.http.get<MarketConfig>(`/api/markets/${code}/onboarding`);
  }

  /** Simulated BankID / MitID login. */
  startIdentitySession(code: string): Observable<string> {
    return this.http
      .post<{ sessionId: string }>(`/api/markets/${code}/identity/sessions`, {})
      .pipe(map((r) => r.sessionId));
  }

  /** Server-side check: checksum, real date, and minimum age. Returns an error message or null. */
  checkNationalId(code: string, value: string): Observable<string | null> {
    return this.http
      .post<{ valid: boolean; message?: string }>(`/api/markets/${code}/national-id/check`, {
        value,
      })
      .pipe(map((r) => (r.valid ? null : (r.message ?? 'Invalid'))));
  }

  submit(code: string, request: ApplicationRequest): Observable<ApplicationResult> {
    return this.http.post<ApplicationResult>(`/api/markets/${code}/applications`, request).pipe(
      // 422 carries a normal result body with field errors.
      catchError((e: HttpErrorResponse) =>
        e.status === 422 ? of(e.error as ApplicationResult) : throwError(() => e),
      ),
    );
  }
}
