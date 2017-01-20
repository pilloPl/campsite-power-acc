import type { components, paths } from './schema';

/** Schemas of the backend (generated from /v3/api-docs with `npm run generate-api`). */
export type Schemas = components['schemas'];

/** Paths of the console operations: every operation is a POST of a JSON request answering a JSON response. */
export type PostPath = {
  [P in keyof paths]: paths[P] extends { post: { requestBody?: unknown } } ? P : never;
}[keyof paths];

type PostOperation<P extends PostPath> = paths[P] extends { post: infer O } ? O : never;

export type RequestOf<P extends PostPath> = PostOperation<P> extends {
  requestBody?: { content: { 'application/json': infer B } };
}
  ? B
  : Record<string, never>;

export type ResponseOf<P extends PostPath> = PostOperation<P> extends {
  responses: { 200: { content: { '*/*': infer R } } };
}
  ? R
  : unknown;

export type CurrentUser = Schemas['CurrentUserDTO'];

/** Fault answered by the backend: `{errorCode, message}` with an HTTP status of 4xx/5xx. */
export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string;

  constructor(status: number, errorCode: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorCode = errorCode;
  }
}

export const XSRF_COOKIE = 'XSRF-TOKEN';
export const XSRF_HEADER = 'X-XSRF-TOKEN';
const CURRENT_USER_PATH = '/api/console/auth/currentUser';

export function readCookie(name: string): string | undefined {
  const prefix = `${name}=`;
  const entry = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));
  return entry ? decodeURIComponent(entry.substring(prefix.length)) : undefined;
}

async function toApiError(response: Response): Promise<ApiError> {
  let errorCode = `HTTP_${response.status}`;
  let message = response.statusText || `Request failed with status ${response.status}`;
  try {
    const body = (await response.json()) as { errorCode?: string; message?: string };
    if (body.errorCode) errorCode = body.errorCode;
    if (body.message) message = body.message;
  } catch {
    // not a JSON fault, keep the status text
  }
  return new ApiError(response.status, errorCode, message);
}

/** The XSRF-TOKEN cookie is set by the backend on every answer; the first call of a session fetches it. */
async function xsrfToken(): Promise<string | undefined> {
  let token = readCookie(XSRF_COOKIE);
  if (!token) {
    await fetch(CURRENT_USER_PATH, { credentials: 'include' });
    token = readCookie(XSRF_COOKIE);
  }
  return token;
}

/** Calls a console operation with the session cookie and the CSRF header. */
export async function post<P extends PostPath>(path: P, body?: RequestOf<P>): Promise<ResponseOf<P>> {
  const token = await xsrfToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json', Accept: 'application/json' };
  if (token) headers[XSRF_HEADER] = token;
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: JSON.stringify(body ?? {}),
  });
  if (!response.ok) throw await toApiError(response);
  const text = await response.text();
  return (text ? JSON.parse(text) : {}) as ResponseOf<P>;
}

/** The logged-in user, or null without a login (the backend answers 401 and sets the XSRF-TOKEN cookie). */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  const response = await fetch(CURRENT_USER_PATH, { credentials: 'include', headers: { Accept: 'application/json' } });
  if (response.status === 401) return null;
  if (!response.ok) throw await toApiError(response);
  const body = (await response.json()) as Schemas['GetCurrentUserResponse'];
  return body.user ?? null;
}

export async function login(username: string, password: string): Promise<CurrentUser> {
  const response = await post('/api/console/auth/login', { username, password });
  if (!response.user) throw new ApiError(500, 'TECHNICAL_ERROR', 'Login answered no user');
  return response.user;
}

export async function logout(): Promise<void> {
  await post('/api/console/auth/logout');
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return String(error);
}
