import { Alert, Box, Button, List, ListItemButton, ListItemText, MenuItem, Stack, TextField, Typography } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { formatNumber, isoDay } from '../components/format';
import { KpiCard } from '../components/KpiCard';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';

type PowerSection = Schemas['PowerSectionDTO'];
type SocketAllocation = Schemas['SocketAllocation'];

const RANGES = [1, 3, 7, 14, 31];
const READS = ['/api/console/powerBudget/', '/api/console/map/'];

function shareColour(share: number): 'error' | 'warning' | 'success' {
  return share >= 1 ? 'error' : share >= 0.8 ? 'warning' : 'success';
}

/** Tab 11: power sections with their capacity, load over time against the capacity and the current allocation. */
export default function PowerBudgetPage() {
  const { hasRight } = useAuth();
  const editReason = !hasRight(Right.PEDESTAL_EDIT) && 'Requires right PEDESTAL_EDIT';
  const sectionsQuery = useApiQuery('/api/console/powerBudget/findPowerSections', {});
  const sections = useMemo(() => sectionsQuery.data?.sections ?? [], [sectionsQuery.data]);
  const algorithms = sectionsQuery.data?.availableAlgorithms ?? [];
  const [selectedId, setSelectedId] = useState<number | undefined>();
  const [days, setDays] = useState(3);
  const [dialog, setDialog] = useState<'capacity' | 'create' | 'edit' | 'assign' | null>(null);
  const [algorithm, setAlgorithm] = useState<string | undefined>();

  const section: PowerSection | undefined = sections.find((s) => s.id === selectedId) ?? sections[0];
  const load = useApiQuery('/api/console/powerBudget/getSectionLoad',
    { sectionId: section?.id, from: isoDay(-(days - 1)), to: isoDay(0) }, { enabled: section?.id !== undefined });

  const close = () => setDialog(null);
  const changeCapacity = useApiMutation('/api/console/powerBudget/changeSectionCapacity', {
    invalidates: READS, success: (r) => `Capacity of ${r.section?.sectionName ?? ''} set to ${formatNumber(r.section?.maxCurrentA, 0)} A`, onSuccess: close,
  });
  const selectAlgorithm = useApiMutation('/api/console/powerBudget/selectAllocationAlgorithm', {
    invalidates: READS, success: (r) => `Allocation algorithm of ${r.section?.sectionName ?? ''}: ${r.section?.algorithmBeanName ?? ''}`,
    onSuccess: () => setAlgorithm(undefined),
  });
  const saveSection = useApiMutation('/api/console/powerBudget/savePowerSection', {
    invalidates: READS, success: (r) => `Section ${r.section?.sectionName ?? ''} saved`,
    onSuccess: (r) => { close(); setSelectedId(r.section?.id); },
  });
  const assignSocket = useApiMutation('/api/console/powerBudget/assignSocketToSection', {
    invalidates: READS, success: (r) => `Socket assigned to ${r.section?.sectionName ?? ''}`, onSuccess: close,
  });

  const allSockets = useMemo(() => sections.flatMap((s) => (s.sockets ?? []).map((socket) => ({ socket, sectionName: s.sectionName }))), [sections]);
  const share = section?.maxCurrentA ? (section.allocatedA ?? 0) / section.maxCurrentA : 0;
  const points = load.data?.load?.points ?? [];
  const capacity = load.data?.load?.maxCurrentA ?? section?.maxCurrentA;

  const columns = useMemo<GridColDef<SocketAllocation>[]>(() => [
    { field: 'pedestalCode', headerName: 'Pedestal', width: 140 },
    { field: 'socketNo', headerName: 'Socket', width: 70 },
    { field: 'pitchNo', headerName: 'Pitch', width: 70 },
    { field: 'status', headerName: 'Status', width: 120, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'ratedCurrentA', headerName: 'Rated A', width: 85, type: 'number' },
    { field: 'allocatedA', headerName: 'Allocated A', width: 100, type: 'number' },
    { field: 'socketCode', headerName: 'Socket code', flex: 1, minWidth: 140 },
  ], []);

  return (
    <Page title="Power budget" toolbar={(
      <Stack direction="row" spacing={1}>
        <ActionButton label="New section" variant="contained" onClick={() => setDialog('create')} disabledReason={editReason} />
        <ActionButton label="Assign socket" onClick={() => setDialog('assign')} disabledReason={editReason || (allSockets.length === 0 && 'No sockets in a section')} />
      </Stack>
    )}>
      <QueryState isLoading={sectionsQuery.isLoading} error={sectionsQuery.error}>
        {sections.length === 0 ? <Alert severity="info">No power section in your scope.</Alert> : (
          <Stack direction="row" spacing={2} alignItems="flex-start">
            <Box sx={{ width: 260, flexShrink: 0 }}>
              <Section title="Sections" testId="section-list">
                <List dense disablePadding>
                  {sections.map((s) => {
                    const sShare = s.maxCurrentA ? (s.allocatedA ?? 0) / s.maxCurrentA : 0;
                    return (
                      <ListItemButton key={s.id} selected={s.id === section?.id} onClick={() => setSelectedId(s.id)}>
                        <ListItemText primary={s.sectionName}
                          secondary={<span>{s.allocatedA ?? 0} / {formatNumber(s.maxCurrentA, 0)} A · <Typography component="span" variant="inherit" color={`${shareColour(sShare)}.main`}>{formatNumber(sShare * 100, 0)} %</Typography></span>} />
                      </ListItemButton>
                    );
                  })}
                </List>
              </Section>
            </Box>
            {section && (
              <Stack spacing={2} sx={{ flex: 1, minWidth: 0 }}>
                <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap data-testid="section-kpis">
                  <KpiCard label="Capacity" value={`${formatNumber(section.maxCurrentA, 0)} A`} />
                  <KpiCard label="Allocated now" value={`${section.allocatedA ?? 0} A`} hint={`${formatNumber(share * 100, 0)} % of the capacity`} tone={shareColour(share)} />
                  <KpiCard label="Running supplies" value={section.activeSessions ?? 0} />
                  <KpiCard label="Sockets" value={section.socketCount ?? 0} />
                  <KpiCard label="Algorithm" value={<Typography variant="body1" sx={{ fontWeight: 500, mt: 0.75, mb: 0.5, wordBreak: 'break-word' }}>{section.algorithmBeanName}</Typography>}
                    hint={section.algorithmAvailable ? 'available' : 'not available – new supplies are refused'} tone={section.algorithmAvailable ? 'default' : 'error'} />
                </Stack>
                <Section title={`${section.sectionName}: load over time`} testId="load-chart"
                  actions={(
                    <TextField select size="small" label="Period" value={days} onChange={(e) => setDays(Number(e.target.value))} sx={{ width: 130 }}>
                      {RANGES.map((d) => <MenuItem key={d} value={d}>{d === 1 ? 'Today' : `Last ${d} days`}</MenuItem>)}
                    </TextField>
                  )}>
                  <QueryState isLoading={load.isLoading} error={load.error}>
                    {points.length === 0 ? <Alert severity="info">No load in the period.</Alert> : (
                      <Chart height={300} option={{
                        tooltip: { trigger: 'axis' },
                        legend: { data: ['Allocated A', 'Measured A', 'Capacity A', 'Supplies'], top: 0 },
                        grid: { left: 50, right: 50, top: 36, bottom: 50 },
                        xAxis: { type: 'time' },
                        yAxis: [{ type: 'value', name: 'A' }, { type: 'value', name: 'supplies', minInterval: 1, splitLine: { show: false } }],
                        dataZoom: [{ type: 'inside' }, { type: 'slider', height: 16, bottom: 8 }],
                        series: [
                          { name: 'Allocated A', type: 'line', step: 'end', showSymbol: false, areaStyle: { opacity: 0.15 }, color: '#1565c0',
                            data: points.map((p) => [p.hour, p.allocatedA ?? 0]) },
                          { name: 'Measured A', type: 'line', showSymbol: false, smooth: true, color: '#2e7d32',
                            data: points.map((p) => [p.hour, p.measuredA ?? 0]) },
                          { name: 'Capacity A', type: 'line', showSymbol: false, color: '#d32f2f', lineStyle: { type: 'dashed', width: 2 },
                            data: points.map((p) => [p.hour, capacity ?? 0]) },
                          { name: 'Supplies', type: 'bar', yAxisIndex: 1, color: 'rgba(199,119,0,0.22)', barMaxWidth: 4,
                            data: points.map((p) => [p.hour, p.sessions ?? 0]) },
                        ],
                      }} />
                    )}
                  </QueryState>
                </Section>
                <Section title="Current allocation per socket" testId="allocation"
                  actions={(
                    <Stack direction="row" spacing={1} alignItems="center">
                      <ActionButton label="Edit section" onClick={() => setDialog('edit')} disabledReason={editReason} />
                      <ActionButton label="Change capacity" onClick={() => setDialog('capacity')} disabledReason={editReason} testId="change-capacity" />
                      <TextField select size="small" label="Allocation algorithm" value={algorithm ?? section.algorithmBeanName ?? ''}
                        onChange={(e) => setAlgorithm(e.target.value)} sx={{ minWidth: 200 }} disabled={Boolean(editReason)}>
                        {[...new Set([...algorithms, section.algorithmBeanName ?? ''])].filter(Boolean).map((name) => (
                          <MenuItem key={name} value={name}>{name}</MenuItem>
                        ))}
                      </TextField>
                      <Button size="small" variant="outlined" disabled={!algorithm || algorithm === section.algorithmBeanName || selectAlgorithm.isPending}
                        onClick={() => selectAlgorithm.mutate({ sectionId: section.id, algorithmBeanName: algorithm })}>Apply</Button>
                    </Stack>
                  )}>
                  <DataTable rows={section.sockets} columns={columns} getRowId={(row) => row.socketId ?? 0} height="auto" hideFooter />
                </Section>
              </Stack>
            )}
          </Stack>
        )}
      </QueryState>
      <FormDialog
        open={dialog === 'capacity'}
        title={`Capacity of ${section?.sectionName ?? ''}`}
        description="Running supplies continue when the capacity falls below the allocation; new supplies are refused until the section fits."
        fields={[{ name: 'maxCurrentA', label: 'Capacity (A)', type: 'number', required: true }]}
        initialValues={{ maxCurrentA: section?.maxCurrentA }}
        busy={changeCapacity.isPending}
        onClose={close}
        onSubmit={(values) => changeCapacity.mutate({ sectionId: section?.id, maxCurrentA: Number(values.maxCurrentA) })}
      />
      <FormDialog
        open={dialog === 'create' || dialog === 'edit'}
        title={dialog === 'edit' ? `Edit ${section?.sectionName ?? ''}` : 'New power section'}
        fields={[
          { name: 'sectionName', label: 'Name', required: true },
          { name: 'maxCurrentA', label: 'Capacity (A)', type: 'number', required: true },
          { name: 'algorithmBeanName', label: 'Allocation algorithm', type: 'select', required: true,
            options: algorithms.map((name) => ({ value: name, label: name })) },
        ]}
        initialValues={dialog === 'edit'
          ? { sectionName: section?.sectionName, maxCurrentA: section?.maxCurrentA, algorithmBeanName: section?.algorithmBeanName }
          : { algorithmBeanName: algorithms[0] }}
        busy={saveSection.isPending}
        onClose={close}
        onSubmit={(values) => saveSection.mutate({
          sectionId: dialog === 'edit' ? section?.id : undefined, sectionName: String(values.sectionName),
          maxCurrentA: Number(values.maxCurrentA), algorithmBeanName: String(values.algorithmBeanName),
        })}
      />
      <FormDialog
        open={dialog === 'assign'}
        title="Assign a socket to a section"
        fields={[
          { name: 'socketId', label: 'Socket', type: 'select', required: true,
            options: allSockets.map(({ socket, sectionName }) => ({
              value: socket.socketId ?? 0, label: `${socket.pedestalCode} socket ${socket.socketNo} (pitch ${socket.pitchNo ?? '–'}, now ${sectionName})`,
            })) },
          { name: 'sectionId', label: 'Section', type: 'select', required: true,
            options: sections.map((s) => ({ value: s.id ?? 0, label: s.sectionName ?? String(s.id) })) },
        ]}
        initialValues={{ sectionId: section?.id }}
        submitLabel="Assign"
        busy={assignSocket.isPending}
        onClose={close}
        onSubmit={(values) => assignSocket.mutate({ socketId: Number(values.socketId), sectionId: Number(values.sectionId) })}
      />
    </Page>
  );
}
