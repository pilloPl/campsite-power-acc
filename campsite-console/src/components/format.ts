/** Formatting of backend values for tables and charts (dates come as ISO strings in the server time zone). */

const dateTime = new Intl.DateTimeFormat('en-GB', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
});
const dateOnly = new Intl.DateTimeFormat('en-GB', { year: 'numeric', month: '2-digit', day: '2-digit' });

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateTime.format(date);
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return '';
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  return Number.isNaN(date.getTime()) ? value : dateOnly.format(date);
}

export function formatNumber(value: number | null | undefined, digits = 2): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '';
  return value.toLocaleString('en-GB', { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

export function formatKwh(value: number | null | undefined): string {
  const text = formatNumber(value, 2);
  return text ? `${text} kWh` : '';
}

export function formatMoney(value: number | null | undefined, currency?: string | null): string {
  const text = formatNumber(value, 2);
  if (!text) return '';
  return currency ? `${text} ${currency}` : text;
}

/** ISO date (yyyy-mm-dd) of a day relative to today, for date-range filters. */
export function isoDay(offsetDays = 0, from: Date = new Date()): string {
  const day = new Date(from.getFullYear(), from.getMonth(), from.getDate() + offsetDays);
  const month = String(day.getMonth() + 1).padStart(2, '0');
  const date = String(day.getDate()).padStart(2, '0');
  return `${day.getFullYear()}-${month}-${date}`;
}

/** Local date-time (yyyy-mm-ddThh:mm:ss) as the backend reads LocalDateTime. */
export function isoLocalDateTime(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** Turns an upper-case enum name into a label: CHECKED_OUT -> Checked out. */
export function enumLabel(value: string | null | undefined): string {
  if (!value) return '';
  const words = value.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}
