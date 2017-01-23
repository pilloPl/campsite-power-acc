import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';
import { Fault, fakeBackend } from './test/backend';
import { renderWithProviders } from './test/render';

const receptionUser = {
  username: 'reception', name: 'Reception Istria', roles: ['RECEPTION'],
  rights: ['CARD_EDIT', 'CARD_VIEW', 'GUEST_EDIT', 'GUEST_VIEW', 'PEDESTAL_VIEW', 'STAY_CHECKOUT'], scopeGroups: [],
};

describe('console login and tabs', () => {
  it('shows the login page without a login', async () => {
    fakeBackend({ '/api/console/auth/currentUser': new Fault(401, 'NOT_AUTHENTICATED', 'Login required') });

    renderWithProviders(<App />, { path: '/dashboard' });

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument();
  });

  it('shows the fault of a refused login', async () => {
    fakeBackend({
      '/api/console/auth/currentUser': new Fault(401, 'NOT_AUTHENTICATED', 'Login required'),
      '/api/console/auth/login': new Fault(401, 'NOT_AUTHENTICATED', 'Wrong username or password'),
    });
    renderWithProviders(<App />);

    await userEvent.type(await screen.findByLabelText(/Username/), 'reception');
    await userEvent.type(screen.getByLabelText(/Password/), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByTestId('login-error')).toHaveTextContent('Wrong username or password');
  });

  it('after the login shows only the tabs of the user rights', async () => {
    fakeBackend({
      '/api/console/auth/currentUser': new Fault(401, 'NOT_AUTHENTICATED', 'Login required'),
      '/api/console/auth/login': { user: receptionUser },
    });
    renderWithProviders(<App />);

    await userEvent.type(await screen.findByLabelText(/Username/), 'reception');
    await userEvent.type(screen.getByLabelText(/Password/), 'demo-reception');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    await waitFor(() => expect(screen.getByTestId('current-user')).toHaveTextContent('Reception Istria'));
    const tabs = screen.getAllByRole('tab').map((tab) => tab.getAttribute('aria-label'));
    expect(tabs).toEqual(['Dashboard', 'Map', 'Cards', 'Guests & stays', 'Supply sessions']);
  });

  it('redirects a tab the user may not see to the first allowed tab', async () => {
    fakeBackend({ '/api/console/auth/currentUser': { user: receptionUser } });

    renderWithProviders(<App />, { path: '/administration' });

    await waitFor(() => expect(screen.getByRole('tab', { selected: true })).toHaveAttribute('aria-label', 'Dashboard'));
  });
});
