import { describe, expect, it } from 'vitest';
import { disclosuresFor, formatMoney, stepForField } from './market';
import { DK } from './test-configs';

describe('disclosuresFor', () => {
  it('shows product-specific confirmations only for that product', () => {
    expect(disclosuresFor(DK, 'aktiesparekonto').map((d) => d.id)).toEqual([
      'terms',
      'ask-one-per-person',
      'news',
    ]);
    expect(disclosuresFor(DK, 'depot').map((d) => d.id)).toEqual(['terms', 'news']);
    expect(disclosuresFor(DK, null).map((d) => d.id)).toEqual(['terms', 'news']);
  });
});

describe('formatMoney', () => {
  it("formats in the market's own locale", () => {
    const dk = formatMoney(174200, { locale: 'da-DK', currency: 'DKK' });
    const se = formatMoney(174200, { locale: 'sv-SE', currency: 'SEK' });
    expect(dk).toContain('174.200');
    expect(dk).not.toContain(','); // whole kroner: no decimal part
    expect(se.replace(/\s/g, ' ')).toContain('174 200');
  });
});

describe('stepForField', () => {
  it('sends each server error back to the step that owns the field', () => {
    expect(stepForField('identity')).toBe('IDENTITY');
    expect(stepForField('nationalId')).toBe('PERSONAL_DETAILS');
    expect(stepForField('productId')).toBe('ACCOUNT_TYPE');
    expect(stepForField('disclosures.terms')).toBe('DISCLOSURES');
  });
});
