import {
  Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Grid, Stack, Table, TableBody, TableCell, TableHead,
  TableRow, Typography,
} from '@mui/material';
import { useState, type ReactNode } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation, useApiQuery } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { ActionButton } from '../../components/ActionButton';
import { useCampsites } from '../../components/CampsiteSelect';
import { FormDialog } from '../../components/FormDialog';
import { enumLabel, formatDate, formatDateTime, isoDay } from '../../components/format';
import { QueryState } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';

function SmallTable({ title, headers, rows, empty, action }: {
  title: string; headers: string[]; rows: ReactNode[][]; empty: string; action?: ReactNode;
}) {
  return (
    <Box>
      <Stack direction="row" alignItems="center" sx={{ mb: 0.5 }}>
        <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>{title}</Typography>
        {action}
      </Stack>
      {rows.length === 0 ? (
        <Typography variant="body2" color="text.secondary">{empty}</Typography>
      ) : (
        <Table size="small">
          <TableHead>
            <TableRow>{headers.map((header) => <TableCell key={header} sx={{ fontWeight: 600 }}>{header}</TableCell>)}</TableRow>
          </TableHead>
          <TableBody>
            {rows.map((cells, index) => (
              <TableRow key={index}>{cells.map((cell, column) => <TableCell key={column}>{cell}</TableCell>)}</TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Box>
  );
}

/** Detail of a guest: units, cards, billing accounts, seasonal contracts, stays and orders; add unit, check-in, cancel order. */
export function GuestDetailDialog({ guestId, onClose }: { guestId: number | null; onClose: () => void }) {
  const { hasRight } = useAuth();
  const { campsites } = useCampsites();
  const [dialog, setDialog] = useState<'unit' | 'checkIn' | null>(null);
  const [cancelling, setCancelling] = useState<Schemas['OrderDTO'] | null>(null);
  const detail = useApiQuery('/api/console/guest/getGuest', { guestId: guestId ?? undefined }, { enabled: guestId !== null });
  const addUnit = useApiMutation('/api/console/guest/addUnit', {
    invalidates: ['/api/console/guest/'], success: 'Unit added', onSuccess: () => setDialog(null),
  });
  const checkIn = useApiMutation('/api/console/guest/checkIn', {
    invalidates: ['/api/console/guest/'],
    success: (response) => `Checked in on pitch ${response.stay?.pitchNo ?? ''}`,
    onSuccess: () => setDialog(null),
  });
  const cancelOrder = useApiMutation('/api/console/guest/cancelOrder', {
    invalidates: ['/api/console/guest/'], success: 'Booking cancelled', onSuccess: () => setCancelling(null),
  });

  const data = detail.data?.guest;
  const guest = data?.guest;
  const canEdit = hasRight(Right.GUEST_EDIT);
  const noEdit = !canEdit && 'Requires the right GUEST_EDIT';
  const openStay = (data?.stays ?? []).find((stay) => !stay.checkOutDate);

  return (
    <Dialog open={guestId !== null} onClose={onClose} fullWidth maxWidth="lg" aria-labelledby="guest-title">
      <DialogTitle id="guest-title">{guest?.fullName ?? 'Guest'}</DialogTitle>
      <DialogContent dividers>
        <QueryState isLoading={detail.isLoading} error={detail.error}>
          <Grid container spacing={1} sx={{ mb: 2 }}>
            {[
              ['Type', enumLabel(guest?.guestType)], ['Status', <StatusChip key="s" status={guest?.status} />],
              ['Issuer', guest?.issuerName], ['E-mail', guest?.email], ['Phone', guest?.phone],
              ['City', `${guest?.city ?? ''} ${guest?.countryIso ?? ''}`], ['Car / trailer', [guest?.carPlate, guest?.trailerPlate].filter(Boolean).join(' / ')],
              ['Payment', enumLabel(guest?.paymentMethod)], ['ERP partner', guest?.erpPartnerNo],
            ].map(([label, value]) => (
              <Grid item key={String(label)} xs={6} md={4}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="body2" component="div">{value || '-'}</Typography>
              </Grid>
            ))}
          </Grid>
          <Stack spacing={2.5}>
            <SmallTable title="Units" headers={['Type', 'Plate', 'Length (m)', 'Description']} empty="No unit."
              action={<ActionButton label="Add unit" onClick={() => setDialog('unit')} disabledReason={noEdit} />}
              rows={(data?.units ?? []).map((unit) => [enumLabel(unit.unitType), unit.plate, unit.lengthM, unit.description])} />
            <SmallTable title="Cards" headers={['Number', 'RFID', 'Status', 'Valid from', 'Valid to', 'Billing account']} empty="No card."
              rows={(data?.cards ?? []).map((card) => [card.cardNumber, card.cardRfid, <StatusChip key="c" status={card.cardStatus} />,
                formatDate(card.validFrom), formatDate(card.validTo), card.billingAccountNo])} />
            <SmallTable title="Billing accounts" headers={['ERP account', 'Description', 'State', 'Created']} empty="No billing account."
              rows={(data?.billingAccounts ?? []).map((account) => [account.erpAccountNo ?? (account.awaitingErpNumber ? 'awaiting ERP number' : ''),
                account.description, account.closed ? 'Closed' : 'Open', formatDate(account.createDate)])} />
            <SmallTable title="Seasonal contracts" headers={['Campsite', 'Pitch', 'Status', 'Start', 'End', 'Tariff']} empty="No seasonal contract."
              rows={(data?.seasonalContracts ?? []).map((contract) => [contract.campsiteName, contract.pitchNo,
                <StatusChip key="s" status={contract.status} />, formatDate(contract.startDate), formatDate(contract.endDate), contract.tariffName])} />
            <SmallTable title="Stays" headers={['Campsite', 'Pitch', 'Arrival', 'Departure', 'Checked in', 'Checked out']} empty="No stay."
              action={<ActionButton label="Check in" variant="contained" onClick={() => setDialog('checkIn')}
                disabledReason={noEdit || (openStay ? 'The guest has an open stay' : false)} testId="guest-checkin" />}
              rows={(data?.stays ?? []).map((stay) => [stay.campsiteName, stay.pitchNo, formatDate(stay.arrivalDate),
                formatDate(stay.departureDate), formatDateTime(stay.checkInDate), formatDateTime(stay.checkOutDate)])} />
            <SmallTable title="Orders and bookings" headers={['Number', 'Status', 'Items', 'Created', 'Activation', '']} empty="No order."
              rows={(data?.orders ?? []).map((order) => [order.orderNumber, <StatusChip key="s" status={order.status} />,
                (order.items ?? []).join(', '), formatDate(order.createDate), formatDate(order.activationDate),
                <ActionButton key="a" label="Cancel" color="error" onClick={() => setCancelling(order)} disabledReason={noEdit} />])} />
          </Stack>
        </QueryState>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
      <FormDialog
        open={dialog === 'unit'}
        title="Add unit"
        fields={[
          { name: 'unitType', label: 'Type', type: 'select', required: true,
            options: ['CAMPER', 'CARAVAN', 'TENT'].map((value) => ({ value, label: enumLabel(value) })) },
          { name: 'plate', label: 'Plate' },
          { name: 'lengthM', label: 'Length (m)', type: 'number' },
        ]}
        initialValues={{ unitType: 'CARAVAN' }}
        submitLabel="Add"
        busy={addUnit.isPending}
        onClose={() => setDialog(null)}
        onSubmit={(values) => addUnit.mutate({ guestId: guest?.id, unitType: values.unitType as string,
          plate: values.plate as string | undefined, lengthM: values.lengthM as number | undefined })}
      />
      <FormDialog
        open={dialog === 'checkIn'}
        title={`Check in ${guest?.fullName ?? ''}`}
        description="The card of the guest becomes valid for the stay."
        fields={[
          { name: 'campsiteGroupId', label: 'Campsite', type: 'select', required: true,
            options: campsites.map((campsite) => ({ value: campsite.groupId, label: campsite.name })) },
          { name: 'pitchNo', label: 'Pitch', required: true },
          { name: 'arrival', label: 'Arrival', type: 'date', required: true },
          { name: 'departure', label: 'Departure', type: 'date', required: true },
          { name: 'cardId', label: 'Card', type: 'select',
            options: (data?.cards ?? []).map((card) => ({ value: card.id ?? 0, label: card.cardNumber ?? String(card.id) })) },
          { name: 'unitId', label: 'Unit', type: 'select',
            options: (data?.units ?? []).map((unit) => ({ value: unit.id ?? 0, label: `${enumLabel(unit.unitType)} ${unit.plate ?? ''}` })) },
          { name: 'billingAccountNo', label: 'Billing account', type: 'select',
            options: (data?.billingAccounts ?? []).filter((account) => account.erpAccountNo && !account.closed)
              .map((account) => ({ value: account.erpAccountNo ?? '', label: account.erpAccountNo ?? '' })) },
        ]}
        initialValues={{
          campsiteGroupId: campsites[0]?.groupId, arrival: isoDay(0), departure: isoDay(7),
          cardId: data?.cards?.[0]?.id, unitId: data?.units?.[0]?.id, billingAccountNo: data?.billingAccounts?.[0]?.erpAccountNo,
        }}
        submitLabel="Check in"
        busy={checkIn.isPending}
        onClose={() => setDialog(null)}
        onSubmit={(values) => checkIn.mutate({
          guestId: guest?.id, campsiteGroupId: values.campsiteGroupId as number, pitchNo: values.pitchNo as string,
          arrival: values.arrival as string, departure: values.departure as string, cardId: values.cardId as number | undefined,
          unitId: values.unitId as number | undefined, billingAccountNo: values.billingAccountNo as string | undefined,
        })}
      />
      <FormDialog
        open={cancelling !== null}
        title={`Cancel booking ${cancelling?.orderNumber ?? ''}`}
        description="Only orders that are not active yet can be cancelled."
        fields={[{ name: 'reason', label: 'Reason', type: 'textarea' }]}
        submitLabel="Cancel booking"
        busy={cancelOrder.isPending}
        onClose={() => setCancelling(null)}
        onSubmit={(values) => cancelOrder.mutate({ orderId: cancelling?.id, reason: values.reason as string | undefined })}
      />
    </Dialog>
  );
}
