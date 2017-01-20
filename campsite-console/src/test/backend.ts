import { vi } from 'vitest';

export interface RecordedCall {
  path: string;
  method: string;
  body: unknown;
  headers: Record<string, string>;
}

export type Handler = (body: any) => unknown;

/** Error answer of a fake operation: `{errorCode, message}` with the status. */
export class Fault {
  constructor(readonly status: number, readonly errorCode: string, readonly message: string) {}
}

/**
 * Replaces fetch with a fake backend: every path answers the handler's result as JSON (a Fault as an error answer).
 * Unknown paths answer an empty object. The XSRF-TOKEN cookie is set like the real backend does.
 */
export function fakeBackend(handlers: Record<string, Handler | unknown>) {
  const calls: RecordedCall[] = [];
  document.cookie = 'XSRF-TOKEN=test-token; path=/';
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = typeof input === 'string' ? input : input instanceof URL ? input.pathname : input.url;
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    calls.push({ path, method: init?.method ?? 'GET', body, headers: (init?.headers ?? {}) as Record<string, string> });
    const handler = handlers[path];
    const result = typeof handler === 'function' ? (handler as Handler)(body) : handler ?? {};
    if (result instanceof Fault) {
      return new Response(JSON.stringify({ errorCode: result.errorCode, message: result.message }), {
        status: result.status, headers: { 'Content-Type': 'application/json' },
      });
    }
    return new Response(JSON.stringify(result), { status: 200, headers: { 'Content-Type': 'application/json' } });
  });
  vi.stubGlobal('fetch', fetchMock);
  return {
    calls,
    callsTo: (path: string) => calls.filter((call) => call.path === path),
  };
}

export const ADMIN_RIGHTS = ['AUDIT_VIEW', 'BILLING_EDIT', 'CARD_EDIT', 'CARD_VIEW', 'CONFIG_EDIT',
  'CONSUMPTION_RECORD_REVIEW', 'GUEST_EDIT', 'GUEST_VIEW', 'PEDESTAL_COMMISSION', 'PEDESTAL_EDIT', 'PEDESTAL_VIEW',
  'PRICE_REPORT_VIEW', 'REPORT_VIEW', 'SOCKET_REMOTE_CONTROL', 'STAY_CHECKOUT', 'TARIFF_EDIT', 'USER_ADMIN'];

export const adminUser = {
  username: 'admin', name: 'Market Admin', status: 'ACTIVE', roles: ['MARKET_ADMIN'], rights: ADMIN_RIGHTS, scopeGroups: [],
};
