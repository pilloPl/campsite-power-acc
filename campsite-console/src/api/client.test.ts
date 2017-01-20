import { ApiError, fetchCurrentUser, login, post } from './client';
import { Fault, fakeBackend } from '../test/backend';

describe('console API client', () => {
  it('posts the request as JSON with the session cookie and the CSRF header from the XSRF-TOKEN cookie', async () => {
    const backend = fakeBackend({ '/api/console/dashboard/getDashboard': { dashboard: { activeSessions: 3 } } });

    const response = await post('/api/console/dashboard/getDashboard', { campsiteGroupId: 7 });

    expect(response.dashboard?.activeSessions).toBe(3);
    const [call] = backend.callsTo('/api/console/dashboard/getDashboard');
    expect(call.method).toBe('POST');
    expect(call.body).toEqual({ campsiteGroupId: 7 });
    expect(call.headers['X-XSRF-TOKEN']).toBe('test-token');
  });

  it('fetches the XSRF-TOKEN cookie first when the session has none yet', async () => {
    const backend = fakeBackend({});
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';

    await post('/api/console/auth/logout');

    expect(backend.calls.map((call) => call.path)).toEqual(['/api/console/auth/currentUser', '/api/console/auth/logout']);
  });

  it('turns a fault of the backend into an ApiError with code and message', async () => {
    fakeBackend({ '/api/console/supply/stopSupply': new Fault(409, 'CONFLICT', 'The pedestal did not accept the stop') });

    const call = post('/api/console/supply/stopSupply', { sessionId: 1 });

    await expect(call).rejects.toBeInstanceOf(ApiError);
    await expect(call).rejects.toMatchObject({ status: 409, errorCode: 'CONFLICT', message: 'The pedestal did not accept the stop' });
  });

  it('answers no current user without a login', async () => {
    fakeBackend({ '/api/console/auth/currentUser': new Fault(401, 'NOT_AUTHENTICATED', 'Login required') });

    await expect(fetchCurrentUser()).resolves.toBeNull();
  });

  it('logs in with username and password and answers the user with rights', async () => {
    const backend = fakeBackend({
      '/api/console/auth/login': (body: { username: string }) => ({ user: { username: body.username, rights: ['CARD_VIEW'] } }),
    });

    const user = await login('reception', 'demo-reception');

    expect(user.rights).toEqual(['CARD_VIEW']);
    expect(backend.callsTo('/api/console/auth/login')[0].body).toEqual({ username: 'reception', password: 'demo-reception' });
  });
});
