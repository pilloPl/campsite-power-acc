import { Alert, Chip, InputAdornment, MenuItem, Stack, TextField } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import type { GridColDef } from '@mui/x-data-grid';
import { useQueries } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { post, type Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { enumLabel, formatDate, formatDateTime, isoDay } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';
import { WelcomePoolDialog } from './cards/WelcomePoolDialog';

type Card = Schemas['CampingCardDTO'];
type HouseCard = Schemas['HouseCardDTO'];


/** Camping cards of the guests matched by the search, or of the guests staying now when the search is empty. */
function useCampingCards(search: string) {
  const term = search.trim();
  const today = isoDay();
  const stays = useApiQuery('/api/console/guest/findStays', { state: 'OPEN', from: today, to: today }, { enabled: term === '' });
  const direct = useApiQuery('/api/console/card/findCards', { rfidOrNumber: term }, { enabled: term !== '' });
  const guests = useApiQuery('/api/console/guest/findGuests', { search: term }, { enabled: term.length >= 2 });
  const guestIds = term === ''
    ? [...new Set((stays.data?.stays ?? []).map((stay) => stay.guestId as number))]
    : (guests.data?.guests ?? []).slice(0, 15).map((guest) => guest.id as number);
  const details = useQueries({
    queries: guestIds.map((guestId) => ({
      queryKey: ['/api/console/guest/getGuest', { guestId }],
      queryFn: () => post('/api/console/guest/getGuest', { guestId }),
    })),
  });
  const cards = new Map<number, Card>();
  (term === '' ? [] : direct.data?.cards ?? []).forEach((card) => cards.set(card.id as number, card));
  details.forEach((detail) => (detail.data?.guest?.cards ?? []).forEach((card) => cards.set(card.id as number, card)));
  const isLoading = (term === '' ? stays.isLoading : direct.isLoading) || details.some((detail) => detail.isLoading);
  return { cards: [...cards.values()], isLoading, error: stays.error ?? direct.error ?? guests.error };
}

/**
 * Tab 5: camping cards (validity, issuer, partner sync) with block / unblock and validity changes, house cards of a
 * campsite owner, and the local card list of a pedestal that can be pushed now.
 */
export default function CardsPage() {
  const { user, hasRight } = useAuth();
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [validityOf, setValidityOf] = useState<Card | null>(null);
  const [issuing, setIssuing] = useState(false);
  const [ownerChoice, setOwner] = useState<string | undefined>();
  const owner = ownerChoice ?? user?.username ?? '';
  const [ownerTyped, setOwnerInput] = useState<string | undefined>();
  const ownerInput = ownerTyped ?? owner;
  const [houseCardEdit, setHouseCardEdit] = useState<HouseCard | null>(null);
  const [pedestalCode, setPedestalCode] = useState<string>('');
  const [checking, setChecking] = useState(false);
  const [assigningWelcome, setAssigningWelcome] = useState(false);
  const [checkResult, setCheckResult] = useState<Schemas['CardCheckDTO'] | null>(null);

  const camping = useCampingCards(search);
  const houseCards = useApiQuery('/api/console/houseCard/findHouseCards', { ownerUsername: owner }, { enabled: owner !== '' });
  const map = useApiQuery('/api/console/map/getMapData', {}, { staleTime: 5 * 60_000 });
  const pedestals = (map.data?.map?.campsites ?? []).flatMap((campsite) =>
    (campsite.pedestals ?? []).map((pedestal) => ({ code: pedestal.code as string, campsite: campsite.displayName, sockets: pedestal.sockets ?? [] })));
  const currentPedestal = pedestalCode || pedestals[0]?.code || '';
  const localList = useApiQuery('/api/console/card/getLocalCardList', { pedestalCode: currentPedestal },
    { enabled: currentPedestal !== '' });

  const cardInvalidates = ['/api/console/card/', '/api/console/guest/getGuest'];
  const changeStatus = useApiMutation('/api/console/card/changeCardStatus', {
    invalidates: cardInvalidates,
    success: (r) => `Card ${r.card?.cardNumber ?? ''} ${r.card?.activation === 'DEACTIVATED' ? 'blocked' : 'unblocked'}`,
  });
  const changeValidity = useApiMutation('/api/console/card/changeCardValidity', {
    invalidates: cardInvalidates, success: 'Card validity changed', onSuccess: () => setValidityOf(null),
  });
  const issueCard = useApiMutation('/api/console/card/issueCard', {
    invalidates: cardInvalidates, success: (r) => `Card ${r.card?.cardNumber ?? ''} issued`, onSuccess: () => setIssuing(false),
  });
  const houseInvalidates = ['/api/console/houseCard/', '/api/console/card/getLocalCardList'];
  const saveHouseCard = useApiMutation('/api/console/houseCard/saveHouseCard', {
    invalidates: houseInvalidates, success: 'House card saved', onSuccess: () => setHouseCardEdit(null),
  });
  const deleteHouseCard = useApiMutation('/api/console/houseCard/deleteHouseCard', { invalidates: houseInvalidates, success: 'House card deleted' });
  const houseCardStatus = useApiMutation('/api/console/houseCard/changeHouseCardStatus', {
    invalidates: houseInvalidates, success: (r) => `House card ${r.active ? 'activated' : 'deactivated'}`,
  });
  const sendList = useApiMutation('/api/console/card/sendLocalCardList', {
    success: (r) => (r.sent ? `Card list sent to ${r.pedestalCode}` : `Card list of ${r.pedestalCode} not sent`),
  });
  const checkCard = useApiMutation('/api/console/card/checkCardAtSocket', {
    onSuccess: (r) => { setCheckResult(r.check ?? null); setChecking(false); },
  });

  const cardColumns = useMemo<GridColDef<Card>[]>(() => [
    { field: 'cardNumber', headerName: 'Card number', width: 120 },
    { field: 'cardRfid', headerName: 'RFID', width: 105 },
    { field: 'guestName', headerName: 'Guest', flex: 1, minWidth: 115 },
    { field: 'issuerGroupName', headerName: 'Issuer', width: 115 },
    { field: 'validFrom', headerName: 'Valid from', width: 95, valueFormatter: (value) => formatDate(value) },
    { field: 'validTo', headerName: 'Valid to', width: 95, valueFormatter: (value) => formatDate(value) },
    { field: 'cardStatus', headerName: 'Validity', width: 150, renderCell: ({ value }) => <StatusChip status={value === 'VALID' ? 'VALID' : value === 'NOT_VALID_ANYMORE' ? 'EXPIRED' : value} label={enumLabel(value)} /> },
    { field: 'activation', headerName: 'Activation', width: 95, renderCell: ({ value }) => <StatusChip status={value === 'DEACTIVATED' ? 'BLOCKED' : 'ACTIVE'} label={value === 'DEACTIVATED' ? 'Blocked' : 'Active'} /> },
    { field: 'partnerSyncState', headerName: 'Partner sync', width: 95, valueFormatter: (value) => enumLabel(value) },
    {
      field: 'actions', headerName: 'Actions', width: 185, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%' }}>
          {row.activation === 'DEACTIVATED' ? (
            <ActionButton label="Unblock" testId={`unblock-${row.id}`}
              onClick={() => changeStatus.mutate({ cardId: row.id, activation: 'ACTIVATED' })} />
          ) : (
            <ActionButton label="Block" color="error" testId={`block-${row.id}`}
              confirm={`Card ${row.cardNumber} of ${row.guestName ?? 'the guest'} will be refused at every pedestal.`}
              onClick={() => changeStatus.mutate({ cardId: row.id, activation: 'DEACTIVATED' })} />
          )}
          <ActionButton label="Validity" onClick={() => setValidityOf(row)} testId={`validity-${row.id}`} />
        </Stack>
      ),
    },
  ], [changeStatus]);

  const houseColumns = useMemo<GridColDef<HouseCard>[]>(() => [
    { field: 'cardNumber', headerName: 'Card number', width: 125, renderCell: ({ row }) => <span title={`RFID ${row.rfid ?? ''}`}>{row.cardNumber}</span> },
    { field: 'houseCardType', headerName: 'Type', width: 70, valueFormatter: (value) => enumLabel(value) },
    { field: 'active', headerName: 'Status', width: 80, renderCell: ({ value }) => <StatusChip status={value ? 'ACTIVE' : 'DEACTIVATED'} label={value ? 'Active' : 'Inactive'} /> },
    { field: 'lastUsed', headerName: 'Last used', flex: 1, minWidth: 120, valueFormatter: (value) => formatDateTime(value) },
    {
      field: 'actions', headerName: 'Actions', width: 235, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%' }}>
          <ActionButton label={row.active ? 'Deactivate' : 'Activate'} testId={`house-status-${row.id}`}
            onClick={() => houseCardStatus.mutate({ ownerUsername: owner, houseCardId: row.id, active: !row.active })} />
          <ActionButton label="Edit" onClick={() => setHouseCardEdit(row)} />
          <ActionButton label="Delete" color="error" confirm={`House card ${row.cardNumber} will be deleted.`}
            onClick={() => deleteHouseCard.mutate({ ownerUsername: owner, houseCardId: row.id })} />
        </Stack>
      ),
    },
  ], [houseCardStatus, deleteHouseCard, owner]);

  const list = localList.data?.localCardList;
  const blocked = camping.cards.filter((card) => card.activation === 'DEACTIVATED').length;
  const valid = camping.cards.filter((card) => card.cardStatus === 'VALID').length;
  const socketsOfPedestal = pedestals.find((pedestal) => pedestal.code === currentPedestal)?.sockets ?? [];

  return (
    <Page
      title="Cards"
      toolbar={(
        <Stack direction="row" spacing={1} alignItems="center">
          <ActionButton label="Issue card" variant="contained" onClick={() => setIssuing(true)} testId="issue-card" />
          <ActionButton label="Assign from welcome pool" onClick={() => setAssigningWelcome(true)}
            disabledReason={!hasRight(Right.CARD_EDIT) && 'Needs the right CARD_EDIT'} testId="assign-welcome" />
          <ActionButton label="Check card at socket" onClick={() => setChecking(true)} testId="check-card" />
        </Stack>
      )}
    >
      {checkResult && (
        <Alert severity={checkResult.validationStatus === 'ACCEPTED' || checkResult.validationStatus === 'VALID' ? 'success' : 'warning'}
          onClose={() => setCheckResult(null)} data-testid="card-check-result">
          Card {checkResult.cardRfid} at socket {checkResult.socket}: <b>{enumLabel(checkResult.validationStatus)}</b>
          {checkResult.guestName ? ` (${checkResult.guestName}${checkResult.houseCard ? ', house card' : ''})` : ''}
        </Alert>
      )}
      <Section
        title={search ? `Camping cards matching "${search}"` : 'Camping cards of the guests staying now'}
        testId="camping-cards"
        actions={(
          <form onSubmit={(e) => { e.preventDefault(); setSearch(searchInput); }}>
            <TextField
              size="small" placeholder="Card number, RFID or guest name" value={searchInput}
              onChange={(e) => { setSearchInput(e.target.value); if (e.target.value === '') setSearch(''); }}
              sx={{ width: 320 }}
              slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> } }}
              inputProps={{ 'aria-label': 'Search cards' }}
            />
          </form>
        )}
      >
        <Stack direction="row" spacing={1} sx={{ mb: 1.5 }} data-testid="card-summary">
          <Chip label={`${camping.cards.length} cards`} />
          <Chip color="success" label={`${valid} valid today`} />
          <Chip color={blocked ? 'error' : 'default'} label={`${blocked} blocked`} />
        </Stack>
        <QueryState isLoading={camping.isLoading} error={camping.error}>
          <DataTable rows={camping.cards} columns={cardColumns} height={330} />
        </QueryState>
      </Section>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} alignItems="flex-start">
        <Section
          title={`House cards${houseCards.data?.houseCards?.[0]?.groupName ? ` of ${houseCards.data.houseCards[0].groupName}` : ''}`}
          flex={3}
          testId="house-cards"
          actions={(
            <Stack direction="row" spacing={1} alignItems="center">
              <form onSubmit={(e) => { e.preventDefault(); setOwner(ownerInput.trim()); }}>
                <TextField size="small" label="Owner (username)" value={ownerInput} onChange={(e) => setOwnerInput(e.target.value)}
                  onBlur={() => setOwner(ownerInput.trim())} sx={{ width: 180 }} />
              </form>
              <ActionButton label="Add house card" onClick={() => setHouseCardEdit({ houseCardType: 'STAFF', active: true })}
                testId="add-house-card" />
            </Stack>
          )}
        >
          <QueryState isLoading={houseCards.isLoading} error={houseCards.error}>
            <DataTable rows={houseCards.data?.houseCards} columns={houseColumns} height={320} />
          </QueryState>
        </Section>
        <Section
          title="Local card list of a pedestal"
          flex={2}
          testId="local-card-list"
          actions={(
            <TextField select size="small" label="Pedestal" value={currentPedestal} sx={{ minWidth: 180 }}
              onChange={(e) => setPedestalCode(e.target.value)} data-testid="pedestal-select">
              {pedestals.map((pedestal) => (
                <MenuItem key={pedestal.code} value={pedestal.code}>{pedestal.code}</MenuItem>
              ))}
            </TextField>
          )}
        >
          <QueryState isLoading={localList.isLoading} error={localList.error}>
            <Stack direction="row" spacing={1} sx={{ mb: 1.5 }} flexWrap="wrap" useFlexGap data-testid="list-summary">
              <Chip label={`capacity ${list?.capacity ?? 0}`} />
              <Chip color="info" label={`${list?.cards ?? 0} guest cards`} />
              <Chip color="info" label={`${list?.houseCards ?? 0} house cards`} />
              <Chip color={list?.droppedEntries ? 'warning' : 'default'} label={`${list?.droppedEntries ?? 0} dropped (over capacity)`} />
            </Stack>
            <DataTable
              rows={(list?.entries ?? []).map((entry, index) => ({ id: index, ...entry }))}
              columns={[
                { field: 'rfid', headerName: 'RFID', flex: 1 },
                { field: 'validTo', headerName: 'Valid to', width: 110, valueFormatter: (value) => formatDate(value) || 'unlimited' },
                { field: 'houseCard', headerName: 'Kind', width: 100, valueFormatter: (value) => (value ? 'House card' : 'Guest card') },
              ]}
              height={238}
            />
          </QueryState>
          <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
            <ActionButton label="Send card list now" variant="contained" busy={sendList.isPending} testId="send-card-list"
              disabledReason={currentPedestal ? null : 'Choose a pedestal'}
              onClick={() => sendList.mutate({ pedestalCode: currentPedestal })} />
          </Stack>
        </Section>
      </Stack>
      <FormDialog
        open={validityOf !== null}
        title={`Validity of card ${validityOf?.cardNumber ?? ''}`}
        description="A card is accepted at the pedestals from the first to the last day of its validity."
        fields={[
          { name: 'validFrom', label: 'Valid from', type: 'date', required: true },
          { name: 'validTo', label: 'Valid to', type: 'date', required: true },
        ]}
        initialValues={{ validFrom: validityOf?.validFrom, validTo: validityOf?.validTo }}
        busy={changeValidity.isPending}
        onClose={() => setValidityOf(null)}
        onSubmit={(values) => changeValidity.mutate({
          cardId: validityOf?.id, validFrom: String(values.validFrom), validTo: String(values.validTo),
        })}
      />
      <FormDialog
        open={issuing}
        title="Issue a camping card"
        fields={[
          { name: 'guestId', label: 'Guest id', type: 'number', required: true, helperText: 'Id of the guest (Guests & stays tab)' },
          { name: 'cardRfid', label: 'Card UID (decimal, from the reader)', required: true },
          { name: 'cardNumber', label: 'Card number', required: true },
          { name: 'validFrom', label: 'Valid from', type: 'date', required: true },
          { name: 'validTo', label: 'Valid to', type: 'date', required: true },
          { name: 'billingAccountNo', label: 'Billing account number' },
        ]}
        initialValues={{ validFrom: isoDay(), validTo: isoDay(7) }}
        submitLabel="Issue"
        busy={issueCard.isPending}
        onClose={() => setIssuing(false)}
        onSubmit={(values) => issueCard.mutate({
          card: {
            guestId: Number(values.guestId), cardRfid: String(values.cardRfid), cardNumber: String(values.cardNumber),
            validFrom: String(values.validFrom), validTo: String(values.validTo),
            billingAccountNo: values.billingAccountNo ? String(values.billingAccountNo) : undefined,
          },
        })}
      />
      <FormDialog
        open={houseCardEdit !== null}
        title={houseCardEdit?.id ? `House card ${houseCardEdit.cardNumber ?? ''}` : 'Add house card'}
        description={`House card of the campsite of ${owner}.`}
        fields={[
          { name: 'houseCardType', label: 'Type', type: 'select', required: true,
            options: [{ value: 'STAFF', label: 'Staff' }, { value: 'RENTAL', label: 'Rental' }] },
          { name: 'rfid', label: 'RFID (decimal UID)', required: true },
          { name: 'cardNumber', label: 'Card number', required: true },
          { name: 'active', label: 'Active', type: 'checkbox' },
        ]}
        initialValues={{ houseCardType: houseCardEdit?.houseCardType, rfid: houseCardEdit?.rfid,
          cardNumber: houseCardEdit?.cardNumber, active: houseCardEdit?.active ?? true }}
        busy={saveHouseCard.isPending}
        onClose={() => setHouseCardEdit(null)}
        onSubmit={(values) => saveHouseCard.mutate({
          ownerUsername: owner,
          houseCard: {
            id: houseCardEdit?.id, houseCardType: String(values.houseCardType), rfid: String(values.rfid),
            cardNumber: String(values.cardNumber), active: Boolean(values.active),
          },
        })}
      />
      <FormDialog
        open={checking}
        title="Check a card at a socket"
        description="Answers what the pedestal would decide for the card at this socket (accepted, unknown, not valid, not accepted at the campsite ...)."
        fields={[
          { name: 'cardRfid', label: 'Card RFID', required: true },
          { name: 'pedestalCode', label: 'Pedestal', type: 'select', required: true,
            options: pedestals.map((pedestal) => ({ value: pedestal.code, label: `${pedestal.code} (${pedestal.campsite ?? ''})` })) },
          { name: 'socketNo', label: 'Socket number', type: 'select', required: true,
            options: (socketsOfPedestal.length ? socketsOfPedestal : [{ socketNo: 1 }, { socketNo: 2 }, { socketNo: 3 }, { socketNo: 4 }])
              .map((socket) => ({ value: socket.socketNo as number, label: String(socket.socketNo) })) },
        ]}
        initialValues={{ pedestalCode: currentPedestal, socketNo: 1 }}
        submitLabel="Check"
        busy={checkCard.isPending}
        onClose={() => setChecking(false)}
        onSubmit={(values) => checkCard.mutate({
          cardRfid: String(values.cardRfid), pedestalCode: String(values.pedestalCode), socketNo: Number(values.socketNo),
        })}
      />
    <WelcomePoolDialog open={assigningWelcome} onClose={() => setAssigningWelcome(false)} />
    </Page>
  );
}
