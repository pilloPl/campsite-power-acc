import { Alert, Stack, Typography } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiQuery } from '../../api/hooks';
import { Chart } from '../../components/Chart';
import { DataTable } from '../../components/DataTable';
import { formatDateTime } from '../../components/format';
import { QueryState, Section } from '../../components/Page';
import { SocketSelect } from './sockets';

type Assignment = Schemas['TariffApplicabilityDTO'];

/** The chain the backend walks to find the tariff of a socket; the first link with a tariff wins. */
export const CHAIN = [
  { source: 'SEASONAL_CONTRACT', label: 'Seasonal contract' },
  { source: 'SOCKET', label: 'Socket' },
  { source: 'PEDESTAL', label: 'Pedestal' },
  { source: 'CAMPSITE_DEFAULT', label: 'Campsite default' },
  { source: 'MARKET_DEFAULT', label: 'Market default' },
];

/** ECharts graph of the chain: links before the source are skipped (grey), the source is green, later ones unused. */
export function chainOption(source: string | undefined, tariffName: string | undefined) {
  const winner = CHAIN.findIndex((link) => link.source === source);
  const colour = (index: number) => (index === winner ? '#2e7d32' : index < winner ? '#bdbdbd' : '#e0e0e0');
  return {
    tooltip: { show: false },
    grid: { left: 60, right: 60, top: 0, bottom: 0 },
    xAxis: { type: 'value' as const, min: 0, max: CHAIN.length - 1, show: false },
    yAxis: { type: 'value' as const, min: -1, max: 1, show: false },
    series: [{
      type: 'graph' as const,
      coordinateSystem: 'cartesian2d',
      symbol: 'roundRect',
      symbolSize: [98, 46],
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 10,
      label: { show: true, fontSize: 11, color: '#212121' },
      data: CHAIN.map((link, index) => ({
        name: link.label,
        value: [index, 0],
        itemStyle: { color: colour(index), borderColor: index === winner ? '#1b5e20' : '#9e9e9e', borderWidth: 1 },
        label: {
          formatter: index === winner && tariffName ? `${link.label}\n${tariffName}` : index < winner ? `${link.label}\nnone` : link.label,
          color: index === winner ? '#fff' : '#424242',
          fontWeight: index === winner ? ('bold' as const) : ('normal' as const),
        },
      })),
      links: CHAIN.slice(1).map((link, index) => ({
        source: CHAIN[index].label,
        target: link.label,
        lineStyle: { color: '#9e9e9e', width: 2 },
      })),
    }],
  };
}

/** Assignment chain of tab 9: which tariff a socket gets now and from which link of the chain. */
export function AssignmentChain({ defaultSocketId }: { defaultSocketId?: number }) {
  const [socketId, setSocketId] = useState<number | undefined>(defaultSocketId);
  const effective = socketId ?? defaultSocketId;
  const query = useApiQuery('/api/console/tariff/findSocketTariff', { socketId: effective }, { enabled: effective !== undefined });
  const result = query.data?.socketTariff;

  const columns: GridColDef<Assignment>[] = [
    { field: 'tariffName', headerName: 'Tariff', width: 160 },
    { field: 'pedestalCode', headerName: 'Pedestal', width: 140 },
    { field: 'socketId', headerName: 'Socket id', width: 110 },
    { field: 'startDate', headerName: 'Valid from', width: 140, valueFormatter: (value) => formatDateTime(value) },
    { field: 'createdBy', headerName: 'Assigned by', flex: 1 },
  ];

  return (
    <Section title="Assignment chain" testId="assignment-chain"
      actions={<SocketSelect value={effective} onChange={setSocketId} testId="chain-socket" />}>
      {effective === undefined ? (
        <Alert severity="info">Choose a socket to see where its tariff comes from.</Alert>
      ) : (
        <QueryState isLoading={query.isLoading} error={query.error}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Socket {result?.socketCode} (pitch {result?.pitchNo}) gets <b>{result?.tariff?.name}</b> ({result?.tariff?.priceText})
            from the {CHAIN.find((link) => link.source === result?.source)?.label.toLowerCase() ?? result?.source}.
          </Typography>
          <Chart option={chainOption(result?.source, result?.tariff?.name)} height={120} testId="chain-chart" />
          <Stack sx={{ mt: 1 }}>
            <DataTable rows={result?.assignments ?? []} columns={columns} height="auto" hideFooter
              localeText={{ noRowsLabel: 'No socket or pedestal assignment, the defaults apply' }} />
          </Stack>
        </QueryState>
      )}
    </Section>
  );
}
