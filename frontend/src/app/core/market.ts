/**
 * Shapes returned by the backend. The UI has no Sweden- or Denmark-specific code:
 * everything market-related arrives as one of these.
 */
export type StepType = 'IDENTITY' | 'PERSONAL_DETAILS' | 'ACCOUNT_TYPE' | 'DISCLOSURES' | 'REVIEW';

export interface MarketSummary {
  code: string;
  displayName: string;
  identityProvider: string;
}

export interface Product {
  id: string;
  name: string;
  summary: string;
  depositCap: number | null;
  onePerPerson: boolean;
}

export interface Disclosure {
  id: string;
  text: string;
  required: boolean;
  productId: string | null;
}

export interface MarketConfig {
  code: string;
  displayName: string;
  locale: string;
  currency: string;
  identityProvider: { id: string; displayName: string };
  nationalId: { label: string; placeholder: string; pattern: string; example: string };
  minimumAge: number;
  steps: StepType[];
  products: Product[];
  disclosures: Disclosure[];
}

export interface ApplicationRequest {
  identitySessionId: string | null;
  nationalId: string;
  fullName: string;
  email: string;
  productId: string | null;
  acceptedDisclosures: string[];
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApplicationResult {
  accepted: boolean;
  applicationId: string | null;
  marketCode: string;
  productId: string | null;
  currency: string | null;
  errors: FieldError[];
}

export const STEP_TITLES: Record<StepType, string> = {
  IDENTITY: 'Verify identity',
  PERSONAL_DETAILS: 'Your details',
  ACCOUNT_TYPE: 'Account type',
  DISCLOSURES: 'Confirmations',
  REVIEW: 'Review',
};

/** Disclosures to show for the chosen product: general ones plus any scoped to that product. */
export function disclosuresFor(config: MarketConfig, productId: string | null): Disclosure[] {
  return config.disclosures.filter((d) => d.productId === null || d.productId === productId);
}

/** Money in the market's own locale and currency, e.g. "174.200 kr." for da-DK. */
export function formatMoney(
  amount: number,
  config: Pick<MarketConfig, 'locale' | 'currency'>,
): string {
  return new Intl.NumberFormat(config.locale, {
    style: 'currency',
    currency: config.currency,
    // Both bounds: SEK and DKK default to 2 decimals, and older browsers reject a max below that.
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

/** Which step a server-side field error should send the user back to. */
export function stepForField(field: string): StepType {
  if (field === 'identity') return 'IDENTITY';
  if (field === 'productId') return 'ACCOUNT_TYPE';
  if (field.startsWith('disclosures.')) return 'DISCLOSURES';
  return 'PERSONAL_DETAILS';
}
