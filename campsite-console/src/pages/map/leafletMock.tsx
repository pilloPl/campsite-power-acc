import type { ReactNode } from 'react';
import { vi } from 'vitest';

/** react-leaflet stand-ins for jsdom: layers render their children, so popups and tooltips are in the DOM. */
export const leafletMock = {
  MapContainer: ({ children }: { children?: ReactNode }) => <div data-testid="leaflet-map">{children}</div>,
  TileLayer: () => null,
  GeoJSON: ({ children }: { children?: ReactNode }) => <div data-testid="geojson">{children}</div>,
  CircleMarker: ({ children }: { children?: ReactNode }) => <div data-testid="pedestal-marker">{children}</div>,
  Popup: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
  Tooltip: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
  useMap: () => ({ fitBounds: vi.fn(), setView: vi.fn() }),
};
