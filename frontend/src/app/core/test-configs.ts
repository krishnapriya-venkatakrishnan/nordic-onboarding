import { MarketConfig } from './market';

/** Fixtures shaped like the real API responses, for component tests. */
export const SE: MarketConfig = {
  code: 'SE',
  displayName: 'Sweden',
  locale: 'sv-SE',
  currency: 'SEK',
  identityProvider: { id: 'bankid', displayName: 'BankID' },
  nationalId: {
    label: 'Personnummer',
    placeholder: 'YYYYMMDD-NNNN',
    pattern: '^(\\d{2})?\\d{6}[-+]?\\d{4}$',
    example: '19121212-1212',
  },
  minimumAge: 18,
  steps: ['IDENTITY', 'PERSONAL_DETAILS', 'ACCOUNT_TYPE', 'DISCLOSURES', 'REVIEW'],
  products: [
    {
      id: 'isk',
      name: 'Investeringssparkonto (ISK)',
      summary: '…',
      depositCap: null,
      onePerPerson: false,
    },
  ],
  disclosures: [{ id: 'terms', text: 'Accept terms', required: true, productId: null }],
};

export const DK: MarketConfig = {
  code: 'DK',
  displayName: 'Denmark',
  locale: 'da-DK',
  currency: 'DKK',
  identityProvider: { id: 'mitid', displayName: 'MitID' },
  nationalId: {
    label: 'CPR number',
    placeholder: 'DDMMYY-SSSS',
    pattern: '^\\d{6}-?\\d{4}$',
    example: '121285-1234',
  },
  minimumAge: 18,
  steps: ['IDENTITY', 'PERSONAL_DETAILS', 'ACCOUNT_TYPE', 'DISCLOSURES', 'REVIEW'],
  products: [
    {
      id: 'aktiesparekonto',
      name: 'Aktiesparekonto',
      summary: '…',
      depositCap: 174200,
      onePerPerson: true,
    },
    {
      id: 'depot',
      name: 'Depot (frie midler)',
      summary: '…',
      depositCap: null,
      onePerPerson: false,
    },
  ],
  disclosures: [
    { id: 'terms', text: 'Accept terms', required: true, productId: null },
    {
      id: 'ask-one-per-person',
      text: 'No other Aktiesparekonto',
      required: true,
      productId: 'aktiesparekonto',
    },
    { id: 'news', text: 'News', required: false, productId: null },
  ],
};
