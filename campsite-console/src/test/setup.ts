import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';
import { createElement } from 'react';

// ECharts needs a canvas; the tests check the option handed to the chart instead.
vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) =>
    createElement('pre', { 'data-testid': 'echarts-option' }, JSON.stringify(option)),
}));

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub as unknown as typeof ResizeObserver;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
});
