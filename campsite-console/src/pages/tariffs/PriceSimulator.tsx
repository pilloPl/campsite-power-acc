import {
  Alert, Box, Button, MenuItem, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation } from '../../api/hooks';
import { enumLabel, formatMoney, formatNumber, isoDay } from '../../components/format';
import { Section } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';
import { SocketSelect } from './sockets';
import { CHAIN } from './AssignmentChain';
import { UNIT_LABEL } from './TariffElements';

type Tariff = Schemas['TariffDTO'];
type Simulation = Schemas['PriceSimulationDTO'];

const withSeconds = (value: string) => (value.length === 16 ? `${value}:00` : value);

/** Price of a hypothetical supply at a socket (its tariff chain, or a chosen tariff), with the priced lines. */
export function PriceSimulator({ tariffs, defaultSocketId }: { tariffs: Tariff[]; defaultSocketId?: number }) {
  const [socketId, setSocketId] = useState<number | undefined>();
  const [tariffId, setTariffId] = useState<number | ''>('');
  const [plugIn, setPlugIn] = useState(`${isoDay(-1)}T18:00`);
  const [plugOut, setPlugOut] = useState(`${isoDay(0)}T08:00`);
  const [kwh, setKwh] = useState('12.5');
  const [nights, setNights] = useState('1');
  const [simulation, setSimulation] = useState<Simulation | null>(null);
  const effectiveSocket = socketId ?? defaultSocketId;

  const simulate = useApiMutation('/api/console/tariff/simulatePrice', {
    onSuccess: (response) => setSimulation(response.simulation ?? null),
  });

  const source = simulation?.tariffSource === 'GIVEN'
    ? 'chosen tariff'
    : CHAIN.find((link) => link.source === simulation?.tariffSource)?.label.toLowerCase() ?? simulation?.tariffSource;

  return (
    <Section title="Price simulator" testId="price-simulator">
      <Stack spacing={1.5}>
        <SocketSelect value={effectiveSocket} onChange={setSocketId} testId="simulator-socket" />
        <TextField select size="small" label="Tariff" value={tariffId}
          onChange={(e) => setTariffId(e.target.value === '' ? '' : Number(e.target.value))}
          slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
          <MenuItem value="">Tariff of the socket (assignment chain)</MenuItem>
          {tariffs.map((tariff) => <MenuItem key={tariff.id} value={tariff.id}>{tariff.name}</MenuItem>)}
        </TextField>
        <Stack direction="row" spacing={1.5}>
          <TextField size="small" type="datetime-local" label="Plug-in" value={plugIn} onChange={(e) => setPlugIn(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} sx={{ flex: 1 }} />
          <TextField size="small" type="datetime-local" label="Plug-out" value={plugOut} onChange={(e) => setPlugOut(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} sx={{ flex: 1 }} />
        </Stack>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <TextField size="small" type="number" label="kWh" value={kwh} onChange={(e) => setKwh(e.target.value)} sx={{ width: 120 }} />
          <TextField size="small" type="number" label="Nights" value={nights} onChange={(e) => setNights(e.target.value)} sx={{ width: 100 }} />
          <Box sx={{ flexGrow: 1 }} />
          <Button variant="contained" disabled={!effectiveSocket || !plugIn || !plugOut || simulate.isPending}
            onClick={() => simulate.mutate({
              socketId: effectiveSocket,
              tariffId: tariffId === '' ? undefined : tariffId,
              plugIn: withSeconds(plugIn),
              plugOut: withSeconds(plugOut),
              kwh: kwh === '' ? undefined : Number(kwh),
              nights: nights === '' ? undefined : Number(nights),
            })}>
            Simulate
          </Button>
        </Stack>
        {simulation && (
          <Box data-testid="simulation-result" sx={{ bgcolor: 'grey.50', borderRadius: 1, p: 1.5 }}>
            <Stack direction="row" spacing={2} alignItems="baseline" sx={{ mb: 1 }}>
              <Typography variant="h4" color="success.main" data-testid="simulation-cost">
                {simulation.costText ?? formatMoney(simulation.cost, simulation.currency)}
              </Typography>
              <StatusChip status={simulation.status} />
              <Typography variant="body2" color="text.secondary">
                {simulation.tariffName} from the {source}
              </Typography>
            </Stack>
            {simulation.failureReason && <Alert severity="warning" sx={{ mb: 1 }}>{simulation.failureReason}</Alert>}
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Element</TableCell>
                  <TableCell>Component</TableCell>
                  <TableCell align="right">Price</TableCell>
                  <TableCell align="right">Share</TableCell>
                  <TableCell align="right">Billed</TableCell>
                  <TableCell align="right">Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(simulation.lines ?? []).map((line, index) => (
                  <TableRow key={index}>
                    <TableCell>{(line.elementPosition ?? 0) + 1}</TableCell>
                    <TableCell>{enumLabel(line.dimensionType)}</TableCell>
                    <TableCell align="right">
                      {formatNumber(line.price, 2)} / {UNIT_LABEL[line.priceUnit ?? ''] ?? line.priceUnit}
                    </TableCell>
                    <TableCell align="right">{formatNumber((line.share ?? 0) * 100, 0)} %</TableCell>
                    <TableCell align="right">{formatNumber(line.billedQuantity, 3)}</TableCell>
                    <TableCell align="right">{formatMoney(line.amount, simulation.currency)}</TableCell>
                  </TableRow>
                ))}
                <TableRow>
                  <TableCell colSpan={5} sx={{ fontWeight: 600 }}>Total</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>{formatMoney(simulation.cost, simulation.currency)}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </Box>
        )}
      </Stack>
    </Section>
  );
}
