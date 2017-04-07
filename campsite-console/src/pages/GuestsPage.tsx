import { Tab, Tabs } from '@mui/material';
import { useState } from 'react';
import { CampsiteSelect } from '../components/CampsiteSelect';
import { Page, Section } from '../components/Page';
import { ContractsTab } from './guests/ContractsTab';
import { GuestsTab } from './guests/GuestsTab';
import { StaysTab } from './guests/StaysTab';

/** Tab 6: stays with check-out (closes the open supply sessions), guests with units, cards and bookings, seasonal contracts. */
export default function GuestsPage() {
  const [view, setView] = useState<'stays' | 'guests' | 'contracts'>('stays');
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  return (
    <Page title="Guests & stays"
      toolbar={view !== 'guests' && <CampsiteSelect value={campsiteGroupId} onChange={setCampsiteGroupId} />}>
      <Section>
        <Tabs value={view} onChange={(_event, value) => setView(value)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Tab value="stays" label="Stays" />
          <Tab value="guests" label="Guests" />
          <Tab value="contracts" label="Seasonal contracts" />
        </Tabs>
        {view === 'stays' && <StaysTab campsiteGroupId={campsiteGroupId} />}
        {view === 'guests' && <GuestsTab />}
        {view === 'contracts' && <ContractsTab campsiteGroupId={campsiteGroupId} />}
      </Section>
    </Page>
  );
}
