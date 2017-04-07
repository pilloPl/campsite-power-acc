import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdministrationPage from './AdministrationPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const trainee = {
  id: 9, username: 'trainee', name: 'Luka Trainee', email: 'trainee@campsite-power.example', status: 'INVITED',
  roles: ['RECEPTION'], scopeGroups: [{ id: 1, name: 'HR ISSUERS', displayName: 'Croatia card issuers' }],
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/user/findUsers': { users: [{ ...adminUser, id: 5, email: 'admin@campsite-power.example' }, trainee] },
    '/api/console/administration/findRoles': { roles: [{ name: 'RECEPTION', selfService: false }, { name: 'MARKET_ADMIN', selfService: false }] },
    '/api/console/user/setPassword': { user: { ...trainee, status: 'ACTIVE' } },
    '/api/console/user/replaceUserRoles': { user: { ...trainee, roles: ['RECEPTION', 'MARKET_ADMIN'] } },
    '/api/console/administration/getRightsMatrix': {
      rightsMatrix: {
        rights: ['CARD_VIEW', 'USER_ADMIN'],
        roles: [{ name: 'RECEPTION', rights: ['CARD_VIEW'] }, { name: 'MARKET_ADMIN', rights: ['CARD_VIEW', 'USER_ADMIN'] }],
        securityGroups: [{ name: 'MARKET ADMINISTRATION', roles: ['MARKET_ADMIN'] }],
      },
    },
    '/api/console/administration/getSystemProperties': {
      properties: [{ key: 'StaleSessionTimeoutInHours', value: '24', description: 'Supply session without a stop is closed' }],
    },
    '/api/console/administration/setSystemProperty': { property: { key: 'StaleSessionTimeoutInHours', value: '12' } },
    '/api/console/administration/reloadConfiguration': { properties: [] },
    '/api/console/job/findJobs': {
      jobs: [{ name: 'closeSessionsOnCheckoutJob', description: 'Closes the supply sessions of checked-out stays',
        intervalSeconds: 300, state: 'NORMAL', running: false, schedulerState: 'STARTED' }],
    },
    '/api/console/job/runJobNow': { job: { name: 'closeSessionsOnCheckoutJob', state: 'NORMAL', running: true } },
    '/api/console/job/pauseJob': { job: { name: 'closeSessionsOnCheckoutJob', state: 'PAUSED' } },
    '/api/console/administration/findAuditEntries': {
      entries: [
        { id: 1, actionType: 'LOGIN', actionName: 'login', userId: 'admin', status: 'SUCCESS' },
        { id: 2, actionType: 'CONSOLE_ACTION', actionName: 'guest/checkOut', userId: 'reception', status: 'SUCCESS', message: '{"stayId":10}' },
      ],
    },
  });
}

describe('administration tab', () => {
  it('sets the first password of an invited user', async () => {
    const calls = backend();
    renderWithProviders(<AdministrationPage />);

    await userEvent.click(await screen.findByTestId('password-trainee'));
    await userEvent.type(screen.getByLabelText(/New password/), 'welcome-2026');
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Set password' }));

    await waitFor(() => expect(calls.callsTo('/api/console/user/setPassword')).toHaveLength(1));
    expect(calls.callsTo('/api/console/user/setPassword')[0].body).toEqual({ username: 'trainee', password: 'welcome-2026' });
  });

  it('does not let the admin lock himself and activates invited users only through a password', async () => {
    backend();
    renderWithProviders(<AdministrationPage />);

    expect(await screen.findByTestId('lock-admin')).toBeDisabled();
    expect(screen.getByTestId('activate-trainee')).toBeDisabled();
  });

  it('replaces the roles of a user', async () => {
    const calls = backend();
    renderWithProviders(<AdministrationPage />);

    await userEvent.click(await screen.findByTestId('roles-trainee'));
    await userEvent.click(await screen.findByLabelText('Market admin'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Replace roles' }));

    await waitFor(() => expect(calls.callsTo('/api/console/user/replaceUserRoles')).toHaveLength(1));
    expect(calls.callsTo('/api/console/user/replaceUserRoles')[0].body).toEqual({ username: 'trainee', roles: ['RECEPTION', 'MARKET_ADMIN'] });
  });

  it('shows the rights matrix of the roles', async () => {
    backend();
    renderWithProviders(<AdministrationPage />);

    await userEvent.click(screen.getByRole('tab', { name: 'Roles & rights' }));

    expect(await screen.findByTestId('held-MARKET_ADMIN-USER_ADMIN')).toBeInTheDocument();
    expect(screen.queryByTestId('held-RECEPTION-USER_ADMIN')).not.toBeInTheDocument();
  });

  it('changes a system property', async () => {
    const calls = backend();
    renderWithProviders(<AdministrationPage />);

    await userEvent.click(screen.getByRole('tab', { name: 'System properties' }));
    await userEvent.click(await screen.findByTestId('edit-StaleSessionTimeoutInHours'));
    const value = screen.getByLabelText('Value');
    await userEvent.clear(value);
    await userEvent.type(value, '12');
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(calls.callsTo('/api/console/administration/setSystemProperty')).toHaveLength(1));
    expect(calls.callsTo('/api/console/administration/setSystemProperty')[0].body).toEqual({
      key: 'StaleSessionTimeoutInHours', value: '12', description: 'Supply session without a stop is closed',
    });
  });

  it('runs and pauses a job', async () => {
    const calls = backend();
    renderWithProviders(<AdministrationPage />);

    await userEvent.click(screen.getByRole('tab', { name: 'Jobs' }));
    await userEvent.click(await screen.findByTestId('run-closeSessionsOnCheckoutJob'));
    await userEvent.click(screen.getByTestId('pause-closeSessionsOnCheckoutJob'));

    await waitFor(() => expect(calls.callsTo('/api/console/job/pauseJob')).toHaveLength(1));
    expect(calls.callsTo('/api/console/job/runJobNow')[0].body).toEqual({ jobName: 'closeSessionsOnCheckoutJob' });
    expect(calls.callsTo('/api/console/job/pauseJob')[0].body).toEqual({ jobName: 'closeSessionsOnCheckoutJob' });
  });

  it('hides the logins in the audit log by default', async () => {
    backend();
    renderWithProviders(<AdministrationPage />);

    await userEvent.click(screen.getByRole('tab', { name: 'Audit log' }));

    expect(await screen.findByText('guest/checkOut')).toBeInTheDocument();
    expect(screen.queryByText('login')).not.toBeInTheDocument();
  });
});
