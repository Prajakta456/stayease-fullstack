import { toLocalISODate, parseLocalDate, addDaysISO, nightsBetween, formatINR } from './format';

describe('date helpers', () => {
  it('uses the local calendar date, not the UTC one', () => {
    // 1:30 AM local time is still "yesterday" in UTC for any time zone east of UTC (e.g. India)
    const earlyMorning = new Date(2026, 9, 14, 1, 30);
    expect(toLocalISODate(earlyMorning)).toBe('2026-10-14');
  });

  it('parses ISO dates as local midnight', () => {
    const date = parseLocalDate('2026-10-14');
    expect([date.getFullYear(), date.getMonth(), date.getDate(), date.getHours()]).toEqual([2026, 9, 14, 0]);
  });

  it('adds days across month and year boundaries', () => {
    expect(addDaysISO('2026-10-31', 1)).toBe('2026-11-01');
    expect(addDaysISO('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDaysISO('2028-02-28', 1)).toBe('2028-02-29');
  });

  it('counts nights, and returns 0 for empty or reversed ranges', () => {
    expect(nightsBetween('2026-10-14', '2026-10-17')).toBe(3);
    expect(nightsBetween('2026-10-14', '2026-10-14')).toBe(0);
    expect(nightsBetween('2026-10-17', '2026-10-14')).toBe(0);
    expect(nightsBetween('', '2026-10-14')).toBe(0);
  });
});

describe('formatINR', () => {
  it('uses Indian digit grouping, no decimals for whole rupees and two otherwise', () => {
    expect(formatINR(1234567)).toBe('₹12,34,567');
    expect(formatINR('2500.00')).toBe('₹2,500');
    expect(formatINR('1437.5')).toBe('₹1,437.50');
    expect(formatINR(99.999)).toBe('₹100');
  });
});
