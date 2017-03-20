import { Chip, type ChipProps } from '@mui/material';
import { enumLabel } from './format';

const COLOURS: Record<string, ChipProps['color']> = {
  AVAILABLE: 'success', ACTIVE: 'success', OPEN: 'info', OK: 'success', CLOSED: 'default', DONE: 'success',
  EXPORTED: 'success', APPROVED: 'success', CHARGED: 'success', SUCCESS: 'success', VALID: 'success', PAID: 'success',
  PRICED: 'success', CONFIRMED: 'success', PROCESSED: 'success', ACCEPTED: 'success',
  OCCUPIED: 'info', SUPPLYING: 'info', RUNNING: 'info', NEW: 'info', PENDING: 'warning', REVIEW: 'warning',
  SUSPECT: 'warning', STALE: 'warning', WARNING: 'warning', LOCKED: 'warning', INVITED: 'warning', PAUSED: 'warning',
  BLOCKED: 'error', FAULTED: 'error', ERROR: 'error', BREAKER_TRIPPED: 'error', REJECTED: 'error', FAILED: 'error',
  UNAVAILABLE: 'error', CRITICAL: 'error', TIMEOUT: 'error', CANCELLED: 'default', DEACTIVATED: 'default',
  CHECKED_OUT: 'default', EXPIRED: 'default', NOT_RUN: 'default', INFO: 'info', HIGH: 'error', MEDIUM: 'warning', LOW: 'info',
};

export function statusColour(status: string | null | undefined): ChipProps['color'] {
  if (!status) return 'default';
  const upper = status.toUpperCase();
  return COLOURS[upper] ?? (upper.includes('ERROR') || upper.includes('FAIL') ? 'error' : 'default');
}

/** Coloured chip of a status value of the backend (socket, session, record, process, user ...). */
export function StatusChip({ status, label }: { status: string | null | undefined; label?: string }) {
  if (!status) return null;
  return <Chip size="small" color={statusColour(status)} label={label ?? enumLabel(status)} variant="filled" />;
}
