import { Box, Chip, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import type { Schemas } from '../../api/client';
import { enumLabel, formatDate, formatNumber } from '../../components/format';

type TariffElement = Schemas['TariffElementDTO'];
type Restriction = Schemas['TariffRestrictionDTO'];

export const UNIT_LABEL: Record<string, string> = {
  PER_KWH: 'kWh', PER_HOUR: 'h', PER_NIGHT: 'night', PER_SESSION: 'session',
};

const hhmm = (time?: string | null) => (time ? time.substring(0, 5) : '');

/** Restriction of a tariff element in words: time window, dates, days of the week, kWh range. */
export function restrictionText(restriction: Restriction | null | undefined): string[] {
  if (!restriction) return [];
  const parts: string[] = [];
  if (restriction.startTime || restriction.endTime) {
    parts.push(`${hhmm(restriction.startTime) || '00:00'}-${hhmm(restriction.endTime) || '24:00'}`);
  }
  if (restriction.startDate || restriction.endDate) {
    parts.push(`${formatDate(restriction.startDate) || '...'} - ${formatDate(restriction.endDate) || '...'}`);
  }
  if (restriction.daysOfWeek && restriction.daysOfWeek.length > 0) {
    parts.push(restriction.daysOfWeek.map((day) => enumLabel(day).substring(0, 3)).join(', '));
  }
  if (restriction.minKwh !== null && restriction.minKwh !== undefined) parts.push(`from ${restriction.minKwh} kWh`);
  if (restriction.maxKwh !== null && restriction.maxKwh !== undefined) parts.push(`up to ${restriction.maxKwh} kWh`);
  return parts;
}

/** Elements of a tariff in evaluation order, each with its restriction and price components. */
export function TariffElements({ elements, currency }: { elements: TariffElement[] | undefined; currency?: string }) {
  const sorted = [...(elements ?? [])].sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
  if (sorted.length === 0) return <Typography color="text.secondary">The tariff has no elements.</Typography>;
  return (
    <Stack spacing={1.5} data-testid="tariff-elements">
      {sorted.map((element, index) => {
        const restriction = restrictionText(element.restriction);
        return (
          <Box key={element.id ?? index} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }} flexWrap="wrap" useFlexGap>
              <Typography variant="subtitle2">Element {(element.position ?? index) + 1}</Typography>
              {restriction.length === 0
                ? <Chip size="small" label="always (fallback)" variant="outlined" />
                : restriction.map((part) => <Chip key={part} size="small" color="info" variant="outlined" label={part} />)}
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Component</TableCell>
                  <TableCell align="right">Price</TableCell>
                  <TableCell>Unit</TableCell>
                  <TableCell align="right">Step</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(element.priceComponents ?? []).map((component, componentIndex) => (
                  <TableRow key={component.id ?? componentIndex}>
                    <TableCell>{enumLabel(component.dimensionType)}</TableCell>
                    <TableCell align="right">{formatNumber(component.price, 2)} {currency}</TableCell>
                    <TableCell>per {UNIT_LABEL[component.priceUnit ?? ''] ?? enumLabel(component.priceUnit)}</TableCell>
                    <TableCell align="right">{component.stepSize ?? 1}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        );
      })}
    </Stack>
  );
}
