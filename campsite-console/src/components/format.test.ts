import { enumLabel, formatKwh, formatMoney, isoDay, isoLocalDateTime } from './format';

describe('formatting of backend values', () => {
  it('shows kWh with two decimals', () => {
    expect(formatKwh(12.3456)).toBe('12.35 kWh');
    expect(formatKwh(undefined)).toBe('');
  });

  it('shows money with the currency', () => {
    expect(formatMoney(4.5, 'EUR')).toBe('4.50 EUR');
  });

  it('turns enum names into labels', () => {
    expect(enumLabel('CHECKED_OUT')).toBe('Checked out');
  });

  it('computes days relative to a date', () => {
    expect(isoDay(-1, new Date(2026, 0, 1))).toBe('2025-12-31');
  });

  it('writes local date-times without a zone', () => {
    expect(isoLocalDateTime(new Date(2026, 6, 4, 9, 5, 0))).toBe('2026-07-04T09:05:00');
  });
});
