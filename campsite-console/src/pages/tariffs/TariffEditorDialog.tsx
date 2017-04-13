import {
  Box, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, IconButton, MenuItem,
  Stack, TextField, Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useEffect, useState } from 'react';
import type { Schemas } from '../../api/client';
import { UNIT_LABEL } from './TariffElements';

type Tariff = Schemas['TariffDTO'];
type TariffElement = Schemas['TariffElementDTO'];
type PriceComponent = Schemas['PriceComponentDTO'];

export const DIMENSION_UNITS: Record<string, string> = {
  ENERGY: 'PER_KWH', TIME: 'PER_HOUR', PER_NIGHT: 'PER_NIGHT', FLAT: 'PER_SESSION',
};
const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const emptyComponent = (): PriceComponent => ({ dimensionType: 'ENERGY', price: 0, stepSize: 1, priceUnit: 'PER_KWH' });
const emptyElement = (position: number): TariffElement => ({ position, priceComponents: [emptyComponent()], restriction: undefined });

export function newTariff(market?: string, currency?: string): Tariff {
  return { name: '', description: '', market: market ?? '', currency: currency ?? 'EUR', elements: [emptyElement(0)] };
}

/** Positions in order, units from the dimension, restriction times as hh:mm:ss. */
function normalise(tariff: Tariff): Tariff {
  return {
    ...tariff,
    elements: (tariff.elements ?? []).map((element, position) => ({
      ...element,
      position,
      priceComponents: (element.priceComponents ?? []).map((component) => ({
        ...component, priceUnit: DIMENSION_UNITS[component.dimensionType ?? 'ENERGY'],
      })),
      restriction: element.restriction ? {
        ...element.restriction,
        startTime: withSeconds(element.restriction.startTime),
        endTime: withSeconds(element.restriction.endTime),
      } : undefined,
    })),
  };
}

function withSeconds(time: string | null | undefined): string | undefined {
  if (!time) return undefined;
  return time.length === 5 ? `${time}:00` : time;
}

const numberOrNull = (raw: string) => (raw === '' ? undefined : Number(raw));

/** Tariff builder: name, market, currency and the elements with restriction and price components. */
export function TariffEditorDialog({ tariff, busy, onSave, onClose }: {
  tariff: Tariff | null;
  busy?: boolean;
  onSave: (tariff: Tariff) => void;
  onClose: () => void;
}) {
  const [draft, setDraft] = useState<Tariff>(newTariff());
  useEffect(() => {
    if (tariff) setDraft(structuredClone(tariff));
  }, [tariff]);

  const elements = draft.elements ?? [];
  const setElement = (index: number, element: TariffElement) =>
    setDraft({ ...draft, elements: elements.map((old, i) => (i === index ? element : old)) });
  const setComponent = (elementIndex: number, componentIndex: number, component: PriceComponent) => {
    const element = elements[elementIndex];
    setElement(elementIndex, {
      ...element,
      priceComponents: (element.priceComponents ?? []).map((old, i) => (i === componentIndex ? component : old)),
    });
  };

  return (
    <Dialog open={tariff !== null} onClose={onClose} fullWidth maxWidth="md" data-testid="tariff-editor">
      <DialogTitle>{draft.id ? `Edit tariff ${draft.name}` : 'New tariff'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Stack direction="row" spacing={2}>
            <TextField label="Name" size="small" required value={draft.name ?? ''} sx={{ flex: 2 }}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
            <TextField label="Market" size="small" required value={draft.market ?? ''} sx={{ flex: 1 }}
              onChange={(e) => setDraft({ ...draft, market: e.target.value.toUpperCase() })} />
            <TextField label="Currency" size="small" required value={draft.currency ?? ''} sx={{ flex: 1 }}
              onChange={(e) => setDraft({ ...draft, currency: e.target.value.toUpperCase() })} />
          </Stack>
          <TextField label="Description" size="small" value={draft.description ?? ''}
            onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
          <Typography variant="body2" color="text.secondary">
            Elements are evaluated in order; the first element whose restriction matches prices that part of the supply.
          </Typography>
          {elements.map((element, elementIndex) => {
            const restriction = element.restriction;
            const setRestriction = (changes: Partial<NonNullable<TariffElement['restriction']>>) =>
              setElement(elementIndex, { ...element, restriction: { ...(restriction ?? {}), ...changes } });
            return (
              <Box key={elementIndex} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
                  <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>Element {elementIndex + 1}</Typography>
                  <FormControlLabel
                    control={<Checkbox size="small" checked={restriction !== null && restriction !== undefined}
                      onChange={(e) => setElement(elementIndex, { ...element, restriction: e.target.checked ? {} : undefined })} />}
                    label="Restricted"
                  />
                  <IconButton aria-label={`Remove element ${elementIndex + 1}`} size="small" disabled={elements.length <= 1}
                    onClick={() => setDraft({ ...draft, elements: elements.filter((_, i) => i !== elementIndex) })}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
                {restriction && (
                  <Stack spacing={1} sx={{ mb: 1.5 }}>
                    <Stack direction="row" spacing={1}>
                      <TextField label="From time" type="time" size="small" value={(restriction.startTime ?? '').substring(0, 5)}
                        onChange={(e) => setRestriction({ startTime: e.target.value || undefined })}
                        slotProps={{ inputLabel: { shrink: true } }} />
                      <TextField label="To time" type="time" size="small" value={(restriction.endTime ?? '').substring(0, 5)}
                        onChange={(e) => setRestriction({ endTime: e.target.value || undefined })}
                        slotProps={{ inputLabel: { shrink: true } }} />
                      <TextField label="From date" type="date" size="small" value={restriction.startDate ?? ''}
                        onChange={(e) => setRestriction({ startDate: e.target.value || undefined })}
                        slotProps={{ inputLabel: { shrink: true } }} />
                      <TextField label="To date" type="date" size="small" value={restriction.endDate ?? ''}
                        onChange={(e) => setRestriction({ endDate: e.target.value || undefined })}
                        slotProps={{ inputLabel: { shrink: true } }} />
                      <TextField label="Min kWh" type="number" size="small" value={restriction.minKwh ?? ''} sx={{ width: 100 }}
                        onChange={(e) => setRestriction({ minKwh: numberOrNull(e.target.value) })} />
                      <TextField label="Max kWh" type="number" size="small" value={restriction.maxKwh ?? ''} sx={{ width: 100 }}
                        onChange={(e) => setRestriction({ maxKwh: numberOrNull(e.target.value) })} />
                    </Stack>
                    <Stack direction="row" flexWrap="wrap">
                      {DAYS.map((day) => (
                        <FormControlLabel key={day} label={day.substring(0, 3)}
                          control={<Checkbox size="small" checked={(restriction.daysOfWeek ?? []).includes(day)}
                            onChange={(e) => setRestriction({
                              daysOfWeek: e.target.checked
                                ? [...(restriction.daysOfWeek ?? []), day]
                                : (restriction.daysOfWeek ?? []).filter((d) => d !== day),
                            })} />} />
                      ))}
                    </Stack>
                  </Stack>
                )}
                {(element.priceComponents ?? []).map((component, componentIndex) => (
                  <Stack key={componentIndex} direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <TextField select label="Component" size="small" value={component.dimensionType ?? 'ENERGY'} sx={{ width: 160 }}
                      onChange={(e) => setComponent(elementIndex, componentIndex, {
                        ...component, dimensionType: e.target.value, priceUnit: DIMENSION_UNITS[e.target.value],
                      })}>
                      {Object.keys(DIMENSION_UNITS).map((dimension) => (
                        <MenuItem key={dimension} value={dimension}>{dimension}</MenuItem>
                      ))}
                    </TextField>
                    <TextField label={`Price (${draft.currency || 'currency'} per ${UNIT_LABEL[DIMENSION_UNITS[component.dimensionType ?? 'ENERGY']]})`}
                      type="number" size="small" value={component.price ?? ''} sx={{ width: 200 }}
                      onChange={(e) => setComponent(elementIndex, componentIndex, { ...component, price: numberOrNull(e.target.value) })} />
                    <TextField label="Step size" type="number" size="small" value={component.stepSize ?? 1} sx={{ width: 100 }}
                      onChange={(e) => setComponent(elementIndex, componentIndex, { ...component, stepSize: numberOrNull(e.target.value) })} />
                    <IconButton aria-label="Remove component" size="small" disabled={(element.priceComponents ?? []).length <= 1}
                      onClick={() => setElement(elementIndex, {
                        ...element, priceComponents: (element.priceComponents ?? []).filter((_, i) => i !== componentIndex),
                      })}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                ))}
                <Button size="small" onClick={() => setElement(elementIndex, {
                  ...element, priceComponents: [...(element.priceComponents ?? []), emptyComponent()],
                })}>
                  Add component
                </Button>
              </Box>
            );
          })}
          <Box>
            <Button size="small" variant="outlined"
              onClick={() => setDraft({ ...draft, elements: [...elements, emptyElement(elements.length)] })}>
              Add element
            </Button>
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={busy || !draft.name || !draft.market || !draft.currency}
          onClick={() => onSave(normalise(draft))}>
          Save tariff
        </Button>
      </DialogActions>
    </Dialog>
  );
}
