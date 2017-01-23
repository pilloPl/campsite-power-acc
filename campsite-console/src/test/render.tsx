import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { Providers, createQueryClient } from '../Providers';

/** Renders a component inside the console providers and a router at the given path. */
export function renderWithProviders(ui: ReactElement, { path = '/' }: { path?: string } = {}) {
  const client = createQueryClient();
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Providers client={client}>{ui}</Providers>
    </MemoryRouter>,
  );
}
