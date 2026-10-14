// Dates are exchanged with the backend as plain ISO dates (YYYY-MM-DD) in the user's local time zone.
// Never use toISOString() for this: it converts to UTC, which in India is still "yesterday" until 5:30 AM.

const pad = (n) => String(n).padStart(2, '0');

export const toLocalISODate = (date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

// Parses YYYY-MM-DD as local midnight (new Date('YYYY-MM-DD') would be UTC midnight)
export const parseLocalDate = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
};

export const todayISO = () => toLocalISODate(new Date());

export const addDaysISO = (iso, days) => {
  const date = parseLocalDate(iso);
  date.setDate(date.getDate() + days);
  return toLocalISODate(date);
};

// Number of nights between two ISO dates; 0 if the range is empty or invalid
export const nightsBetween = (startISO, endISO) => {
  if (!startISO || !endISO) return 0;
  const nights = Math.round((parseLocalDate(endISO) - parseLocalDate(startISO)) / 86400000);
  return nights > 0 ? nights : 0;
};

export const formatDate = (iso, options = { day: 'numeric', month: 'short', year: 'numeric' }) =>
  parseLocalDate(iso).toLocaleDateString('en-IN', options);

// Whole rupees without decimals (₹2,500), otherwise always two decimals (₹2,687.50)
export const formatINR = (amount) => {
  const value = Math.round(Number(amount) * 100) / 100;
  const digits = Number.isInteger(value) ? 0 : 2;
  return `₹${value.toLocaleString('en-IN', { minimumFractionDigits: digits, maximumFractionDigits: digits })}`;
};
