import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, MenuItem, Stack, TextField,
} from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useEffect, useMemo, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation, useApiQuery } from '../../api/hooks';
import { DataTable } from '../../components/DataTable';
import { formatDate, isoDay } from '../../components/format';
import { QueryState } from '../../components/Page';

type Card = Schemas['CampingCardDTO'];
type Stay = Schemas['StayDTO'];

/**
 * Hands a free card of the welcome card pool to a guest staying now (a guest who arrived without a card): the card
 * leaves the pool, belongs to the guest for the stay and bills to the guest's account.
 */
export function WelcomePoolDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const today = isoDay();
  const pool = useApiQuery('/api/console/card/findWelcomePoolCards', {}, { enabled: open });
  const stays = useApiQuery('/api/console/guest/findStays', { state: 'OPEN', from: today, to: today }, { enabled: open });
  const [cardId, setCardId] = useState<number | undefined>();
  const [stayId, setStayId] = useState<number | undefined>();
  const [validFrom, setValidFrom] = useState(today);
  const [validTo, setValidTo] = useState('');
  const assign = useApiMutation('/api/console/card/assignWelcomeCard', {
    invalidates: ['/api/console/card/', '/api/console/guest/'],
    success: (response) => `Card ${response.card?.cardNumber ?? ''} handed to ${response.card?.guestName ?? 'the guest'}`,
    onSuccess: onClose,
  });

  const cards = pool.data?.cards ?? [];
  const staysNow = useMemo(
    () => [...(stays.data?.stays ?? [])].sort((a, b) => (a.guestName ?? '').localeCompare(b.guestName ?? '')),
    [stays.data],
  );
  const stay: Stay | undefined = staysNow.find((candidate) => candidate.id === stayId);

  useEffect(() => {
    if (!open) return;
    setCardId(undefined);
    setStayId(undefined);
    setValidFrom(today);
    setValidTo('');
  }, [open, today]);

  useEffect(() => {
    if (cardId === undefined && cards.length > 0) setCardId(cards[0].id);
  }, [cards, cardId]);

  const columns = useMemo<GridColDef<Card>[]>(() => [
    { field: 'cardNumber', headerName: 'Card number', width: 130 },
    { field: 'cardRfid', headerName: 'RFID', width: 130 },
    { field: 'issuerGroupName', headerName: 'Pool', flex: 1, minWidth: 180 },
  ], []);

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md" data-testid="welcome-pool-dialog">
      <DialogTitle>Assign a card from the welcome pool</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Welcome cards are kept for guests who arrive without a card. The card leaves the pool, belongs to the guest
          for the stay and bills to the guest&apos;s billing account; the card issuer partners get it as a new card.
        </DialogContentText>
        <QueryState isLoading={pool.isLoading} error={pool.error}>
          {cards.length === 0 ? (
            <Alert severity="info">No free card in the welcome card pools of your scope.</Alert>
          ) : (
            <DataTable
              rows={cards}
              columns={columns}
              height={250}
              rowSelectionModel={cardId !== undefined ? [cardId] : []}
              onRowClick={(params) => setCardId(params.row.id)}
              testId="welcome-pool-cards"
              initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
              sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' } }}
            />
          )}
        </QueryState>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mt: 2 }}>
          <TextField
            select size="small" label="Guest staying now" value={stayId ?? ''} sx={{ flex: 2 }}
            onChange={(e) => {
              const id = Number(e.target.value);
              setStayId(id);
              const chosen = staysNow.find((candidate) => candidate.id === id);
              if (chosen?.departureDate) setValidTo(chosen.departureDate);
            }}
            slotProps={{ htmlInput: { 'data-testid': 'welcome-guest' } }}
          >
            {staysNow.map((candidate) => (
              <MenuItem key={candidate.id} value={candidate.id}>
                {candidate.guestName} — {candidate.campsiteName}, pitch {candidate.pitchNo}, until {formatDate(candidate.departureDate)}
              </MenuItem>
            ))}
          </TextField>
          <TextField size="small" type="date" label="Valid from" value={validFrom} onChange={(e) => setValidFrom(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} sx={{ flex: 1 }} />
          <TextField size="small" type="date" label="Valid to" value={validTo} onChange={(e) => setValidTo(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} sx={{ flex: 1 }} />
        </Stack>
        {stay?.cardNumber && (
          <Alert severity="info" sx={{ mt: 2 }}>
            {stay.guestName} has card {stay.cardNumber} already; the welcome card is an additional card.
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          disabled={cardId === undefined || !stay || !validTo || assign.isPending}
          onClick={() => assign.mutate({ cardId, guestId: stay?.guestId, validFrom, validTo })}
          data-testid="welcome-assign"
        >
          Assign card
        </Button>
      </DialogActions>
    </Dialog>
  );
}
