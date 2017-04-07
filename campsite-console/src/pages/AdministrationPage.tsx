import { Box, Checkbox, Chip, FormControlLabel, FormGroup, InputAdornment, Stack, Tab, Tabs, TextField, Tooltip } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import SearchIcon from '@mui/icons-material/Search';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { ActionButton } from '../components/ActionButton';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { enumLabel, formatDateTime } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';

type User = Schemas['UserinfoDTO'];
type Property = Schemas['SystemPropertyDTO'];
type Job = Schemas['JobDTO'];
type AuditEntry = Schemas['AuditEntryDTO'];

const SECTIONS = [
  { value: 'users', label: 'Users' },
  { value: 'rights', label: 'Roles & rights' },
  { value: 'properties', label: 'System properties' },
  { value: 'audit', label: 'Audit log' },
  { value: 'jobs', label: 'Jobs' },
] as const;
type SectionName = (typeof SECTIONS)[number]['value'];

function SearchField({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  return (
    <TextField
      size="small" placeholder={placeholder} value={value} onChange={(e) => onChange(e.target.value)} sx={{ width: 280 }}
      slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> } }}
      inputProps={{ 'aria-label': placeholder }}
    />
  );
}

function UsersSection() {
  const { user: me } = useAuth();
  const [search, setSearch] = useState('');
  const [creating, setCreating] = useState(false);
  const [passwordOf, setPasswordOf] = useState<User | null>(null);
  const [editing, setEditing] = useState<User | null>(null);
  const [rolesOf, setRolesOf] = useState<User | null>(null);
  const [roles, setRoles] = useState<string[]>([]);

  const users = useApiQuery('/api/console/user/findUsers', { search });
  const allRoles = useApiQuery('/api/console/administration/findRoles', {});
  const invalidates = ['/api/console/user/'];
  const createUser = useApiMutation('/api/console/user/createUser', {
    invalidates, success: (r) => `User ${r.user?.username ?? ''} invited`, onSuccess: () => setCreating(false),
  });
  const setPassword = useApiMutation('/api/console/user/setPassword', {
    invalidates, success: 'Password set', onSuccess: () => setPasswordOf(null),
  });
  const updateUser = useApiMutation('/api/console/user/updateUser', {
    invalidates, success: 'User saved', onSuccess: () => setEditing(null),
  });
  const changeStatus = useApiMutation('/api/console/user/changeUserStatus', {
    invalidates, success: (r) => `User ${r.user?.username ?? ''} is now ${enumLabel(r.user?.status)}`,
  });
  const replaceRoles = useApiMutation('/api/console/user/replaceUserRoles', {
    invalidates, success: 'Roles replaced', onSuccess: () => setRolesOf(null),
  });

  const columns = useMemo<GridColDef<User>[]>(() => [
    { field: 'username', headerName: 'Username', width: 120 },
    { field: 'name', headerName: 'Name', width: 150 },
    { field: 'email', headerName: 'E-mail', flex: 1, minWidth: 170 },
    { field: 'status', headerName: 'Status', width: 105, renderCell: ({ value }) => <StatusChip status={value} /> },
    {
      field: 'roles', headerName: 'Roles', width: 150, sortable: false,
      renderCell: ({ row }) => (row.roles ?? []).map((role) => <Chip key={role} size="small" label={enumLabel(role)} sx={{ mr: 0.5 }} />),
    },
    {
      field: 'scopeGroups', headerName: 'Scope', width: 140, sortable: false,
      valueGetter: (_value, row) => (row.scopeGroups ?? []).map((group) => group.displayName || group.name).join(', '),
    },
    {
      field: 'actions', headerName: 'Actions', width: 320, sortable: false,
      renderCell: ({ row }) => {
        const self = row.username === me?.username;
        return (
          <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%' }}>
            <ActionButton label="Edit" onClick={() => setEditing(row)} />
            <ActionButton label="Password" onClick={() => setPasswordOf(row)} testId={`password-${row.username}`} />
            <ActionButton label="Roles" onClick={() => { setRolesOf(row); setRoles(row.roles ?? []); }}
              testId={`roles-${row.username}`} />
            {row.status === 'ACTIVE' ? (
              <ActionButton label="Lock" color="warning" disabledReason={self ? 'You cannot lock yourself' : null}
                confirm={`${row.username} will not be able to log in.`} testId={`lock-${row.username}`}
                onClick={() => changeStatus.mutate({ username: row.username, status: 'LOCKED' })} />
            ) : (
              <ActionButton label="Activate" disabledReason={row.status === 'INVITED' ? 'Set a password to activate an invited user' : null}
                testId={`activate-${row.username}`}
                onClick={() => changeStatus.mutate({ username: row.username, status: 'ACTIVE' })} />
            )}
          </Stack>
        );
      },
    },
  ], [me, changeStatus]);

  return (
    <Section
      title="Console users"
      testId="users"
      actions={(
        <Stack direction="row" spacing={1}>
          <SearchField value={search} onChange={setSearch} placeholder="Username, name or e-mail" />
          <ActionButton label="Invite user" variant="contained" onClick={() => setCreating(true)} testId="invite-user" />
        </Stack>
      )}
    >
      <QueryState isLoading={users.isLoading} error={users.error}>
        <DataTable rows={users.data?.users} columns={columns} height="auto" />
      </QueryState>
      <FormDialog
        open={creating}
        title="Invite a console user"
        description="The user is created as INVITED; an administrator sets the first password, which activates the user."
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'email', label: 'E-mail', required: true },
          { name: 'market', label: 'Market', type: 'select', required: true,
            options: [{ value: 'HR', label: 'Croatia' }, { value: 'IT', label: 'Italy' }, { value: 'FR', label: 'France' }] },
        ]}
        initialValues={{ market: 'HR' }}
        submitLabel="Invite"
        busy={createUser.isPending}
        onClose={() => setCreating(false)}
        onSubmit={(values) => createUser.mutate({ name: String(values.name), email: String(values.email), market: String(values.market) })}
      />
      <FormDialog
        open={passwordOf !== null}
        title={`Set the password of ${passwordOf?.username ?? ''}`}
        description="At least 8 characters and not the username. An invited user becomes active."
        fields={[{ name: 'password', label: 'New password', type: 'password', required: true }]}
        submitLabel="Set password"
        busy={setPassword.isPending}
        onClose={() => setPasswordOf(null)}
        onSubmit={(values) => setPassword.mutate({ username: passwordOf?.username, password: String(values.password) })}
      />
      <FormDialog
        open={editing !== null}
        title={`User ${editing?.username ?? ''}`}
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'email', label: 'E-mail', required: true },
        ]}
        initialValues={{ name: editing?.name, email: editing?.email }}
        busy={updateUser.isPending}
        onClose={() => setEditing(null)}
        onSubmit={(values) => updateUser.mutate({ username: editing?.username, name: String(values.name), email: String(values.email) })}
      />
      <FormDialog
        open={rolesOf !== null}
        title={`Roles of ${rolesOf?.username ?? ''}`}
        description="Only roles you hold yourself can be granted."
        fields={[]}
        submitLabel="Replace roles"
        busy={replaceRoles.isPending}
        onClose={() => setRolesOf(null)}
        onSubmit={() => replaceRoles.mutate({ username: rolesOf?.username, roles })}
      >
        <FormGroup>
          {(allRoles.data?.roles ?? []).filter((role) => !role.selfService).map((role) => (
            <FormControlLabel
              key={role.name}
              label={enumLabel(role.name)}
              control={(
                <Checkbox
                  checked={roles.includes(role.name ?? '')}
                  onChange={(e) => setRoles((old) => (e.target.checked ? [...old, role.name ?? ''] : old.filter((name) => name !== role.name)))}
                />
              )}
            />
          ))}
        </FormGroup>
      </FormDialog>
    </Section>
  );
}

function RightsSection() {
  const matrix = useApiQuery('/api/console/administration/getRightsMatrix', {});
  const data = matrix.data?.rightsMatrix;
  const roles = data?.roles ?? [];
  const rights = data?.rights ?? [];
  return (
    <Stack spacing={2}>
      <Section title="Rights of the roles" testId="rights-matrix">
        <QueryState isLoading={matrix.isLoading} error={matrix.error}>
          <Box sx={{ overflowX: 'auto' }}>
            <Box component="table" sx={{ borderCollapse: 'collapse', fontSize: 12, '& td, & th': { border: 1, borderColor: 'divider', px: 0.75, py: 0.5 } }}>
              <thead>
                <tr>
                  <Box component="th" sx={{ textAlign: 'left', minWidth: 170 }}>Role</Box>
                  {rights.map((right) => (
                    <Box component="th" key={right} sx={{ height: 150, verticalAlign: 'bottom', whiteSpace: 'nowrap', p: 0 }}>
                      <Box sx={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)', px: 0.5, fontWeight: 500 }}>{enumLabel(right)}</Box>
                    </Box>
                  ))}
                </tr>
              </thead>
              <tbody>
                {roles.map((role) => (
                  <tr key={role.name}>
                    <Box component="td" sx={{ fontWeight: 500, whiteSpace: 'nowrap' }}>
                      {enumLabel(role.name)}
                      {role.selfService && <Chip size="small" label="self-service" sx={{ ml: 1 }} />}
                    </Box>
                    {rights.map((right) => {
                      const held = (role.rights ?? []).includes(right);
                      return (
                        <Box component="td" key={right} sx={{ textAlign: 'center', bgcolor: held ? 'rgba(46,107,63,0.12)' : undefined }}
                          data-testid={held ? `held-${role.name}-${right}` : undefined}>
                          {held && <CheckIcon sx={{ fontSize: 16, color: 'primary.main' }} />}
                        </Box>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </Box>
          </Box>
        </QueryState>
      </Section>
      <Section title="Security groups (logic in the group names)">
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {(data?.securityGroups ?? []).map((group) => (
            <Tooltip key={group.name} title={(group.roles ?? []).map(enumLabel).join(', ')}>
              <Chip variant="outlined" label={`${group.name} → ${(group.roles ?? []).map(enumLabel).join(', ')}`} />
            </Tooltip>
          ))}
        </Stack>
      </Section>
    </Stack>
  );
}

function PropertiesSection() {
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<Property | null>(null);
  const properties = useApiQuery('/api/console/administration/getSystemProperties', {});
  const invalidates = ['/api/console/administration/getSystemProperties'];
  const setProperty = useApiMutation('/api/console/administration/setSystemProperty', {
    invalidates, success: (r) => `${r.property?.key ?? 'Property'} saved`, onSuccess: () => setEditing(null),
  });
  const reload = useApiMutation('/api/console/administration/reloadConfiguration', { invalidates, success: 'Configuration reloaded' });
  const term = search.trim().toLowerCase();
  const rows = (properties.data?.properties ?? [])
    .filter((property) => !term || `${property.key} ${property.value ?? ''} ${property.description ?? ''}`.toLowerCase().includes(term))
    .map((property) => ({ id: property.key, ...property }));
  const columns: GridColDef<Property & { id?: string }>[] = [
    { field: 'key', headerName: 'Key', width: 330 },
    { field: 'value', headerName: 'Value', width: 260, renderCell: ({ value }) => value ?? <Box component="span" sx={{ color: 'text.disabled' }}>default</Box> },
    { field: 'description', headerName: 'Description', flex: 1, minWidth: 250 },
    {
      field: 'actions', headerName: '', width: 90, sortable: false,
      renderCell: ({ row }) => <ActionButton label="Edit" onClick={() => setEditing(row)} testId={`edit-${row.key}`} />,
    },
  ];
  return (
    <Section
      title="System properties (SYSTEM_PROPERTY)"
      testId="system-properties"
      actions={(
        <Stack direction="row" spacing={1}>
          <SearchField value={search} onChange={setSearch} placeholder="Filter properties" />
          <ActionButton label="Reload configuration" variant="contained" busy={reload.isPending} testId="reload-configuration"
            confirm="The application re-reads every system property from the database."
            onClick={() => reload.mutate({})} />
        </Stack>
      )}
    >
      <QueryState isLoading={properties.isLoading} error={properties.error}>
        <DataTable rows={rows} columns={columns} height={600} />
      </QueryState>
      <FormDialog
        open={editing !== null}
        title={editing?.key ?? ''}
        description="Empty value = the coded default. The change is audited (CONFIG_CHANGE)."
        fields={[
          { name: 'value', label: 'Value' },
          { name: 'description', label: 'Description', type: 'textarea' },
        ]}
        initialValues={{ value: editing?.value ?? '', description: editing?.description ?? '' }}
        busy={setProperty.isPending}
        onClose={() => setEditing(null)}
        onSubmit={(values) => setProperty.mutate({
          key: editing?.key, value: String(values.value ?? ''), description: values.description ? String(values.description) : undefined,
        })}
      />
    </Section>
  );
}

function AuditSection() {
  const [search, setSearch] = useState('');
  const [actionType, setActionType] = useState('');
  const [hideLogins, setHideLogins] = useState(true);
  const audit = useApiQuery('/api/console/administration/findAuditEntries', {
    freeSearch: search || undefined, type: actionType || undefined, sortColumn: 'actionDate', sortOrder: 'desc',
  });
  const allEntries = audit.data?.entries ?? [];
  const entries = hideLogins && !actionType ? allEntries.filter((entry) => entry.actionType !== 'LOGIN') : allEntries;
  const columns: GridColDef<AuditEntry>[] = [
    { field: 'actionDate', headerName: 'Time', width: 130, valueFormatter: (value) => formatDateTime(value) },
    { field: 'userId', headerName: 'User', width: 110 },
    { field: 'actionType', headerName: 'Type', width: 130 },
    { field: 'actionName', headerName: 'Action', width: 240 },
    { field: 'status', headerName: 'Status', width: 95, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'message', headerName: 'Message', flex: 1, minWidth: 250 },
    { field: 'errorReason', headerName: 'Error', width: 180 },
  ];
  const types = [...new Set(allEntries.map((entry) => entry.actionType).filter(Boolean))] as string[];
  return (
    <Section
      title="Audit log"
      testId="audit-log"
      actions={(
        <Stack direction="row" spacing={1}>
          <TextField select size="small" label="Type" value={actionType} onChange={(e) => setActionType(e.target.value)}
            sx={{ minWidth: 160 }} SelectProps={{ native: true }} slotProps={{ inputLabel: { shrink: true } }}>
            <option value="">All types</option>
            {[...new Set(['LOGIN', 'CONSOLE_ACTION', 'CONFIG_CHANGE', ...types])].map((type) => <option key={type} value={type}>{type}</option>)}
          </TextField>
          <FormControlLabel control={<Checkbox checked={hideLogins} onChange={(e) => setHideLogins(e.target.checked)} />}
            label="Hide logins" />
          <SearchField value={search} onChange={setSearch} placeholder="Search user, action or message" />
        </Stack>
      )}
    >
      <QueryState isLoading={audit.isLoading} error={audit.error}>
        <DataTable rows={entries} columns={columns} height={600} />
      </QueryState>
    </Section>
  );
}

function JobsSection() {
  const jobs = useApiQuery('/api/console/job/findJobs', {}, { refetchInterval: 15_000 });
  const invalidates = ['/api/console/job/'];
  const runNow = useApiMutation('/api/console/job/runJobNow', { invalidates, success: (r) => `${r.job?.name ?? 'Job'} started` });
  const pause = useApiMutation('/api/console/job/pauseJob', { invalidates, success: (r) => `${r.job?.name ?? 'Job'} paused` });
  const resume = useApiMutation('/api/console/job/resumeJob', { invalidates, success: (r) => `${r.job?.name ?? 'Job'} resumed` });
  const columns: GridColDef<Job>[] = [
    { field: 'name', headerName: 'Job', width: 250 },
    { field: 'description', headerName: 'Description', flex: 1, minWidth: 250 },
    {
      field: 'intervalSeconds', headerName: 'Interval', width: 90,
      valueFormatter: (value: number | undefined) => (value === undefined || value === null ? ''
        : value >= 3600 ? `${value / 3600} h` : value >= 60 ? `${value / 60} min` : `${value} s`),
    },
    { field: 'state', headerName: 'State', width: 105, renderCell: ({ row }) => <StatusChip status={row.running ? 'RUNNING' : row.state === 'NORMAL' ? 'ACTIVE' : row.state} label={row.running ? 'Running' : enumLabel(row.state)} /> },
    { field: 'previousFireTime', headerName: 'Last run', width: 130, valueFormatter: (value) => formatDateTime(value) },
    { field: 'nextFireTime', headerName: 'Next run', width: 130, valueFormatter: (value) => formatDateTime(value) },
    {
      field: 'actions', headerName: 'Actions', width: 190, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%' }}>
          <ActionButton label="Run now" testId={`run-${row.name}`}
            disabledReason={row.state === 'PAUSED' ? 'Resume the job first' : row.running ? 'The job is running' : null}
            onClick={() => runNow.mutate({ jobName: row.name })} />
          {row.state === 'PAUSED' ? (
            <ActionButton label="Resume" onClick={() => resume.mutate({ jobName: row.name })} testId={`resume-${row.name}`} />
          ) : (
            <ActionButton label="Pause" color="warning" onClick={() => pause.mutate({ jobName: row.name })} testId={`pause-${row.name}`} />
          )}
        </Stack>
      ),
    },
  ];
  const scheduler = jobs.data?.jobs?.[0]?.schedulerState;
  return (
    <Section
      title="Scheduled jobs"
      testId="jobs"
      actions={scheduler ? <Chip size="small" color={scheduler === 'STARTED' ? 'success' : 'warning'} label={`Scheduler ${enumLabel(scheduler)}`} /> : undefined}
    >
      <QueryState isLoading={jobs.isLoading} error={jobs.error}>
        <DataTable rows={jobs.data?.jobs} columns={columns} getRowId={(row) => row.name ?? ''} height={600} />
      </QueryState>
    </Section>
  );
}

/** Tab 16: users, roles and the rights matrix, system properties, audit log and the scheduled jobs. */
export default function AdministrationPage() {
  const [section, setSection] = useState<SectionName>('users');
  return (
    <Page title="Administration">
      <Tabs value={section} onChange={(_, value: SectionName) => setSection(value)} aria-label="Administration sections"
        sx={{ borderBottom: 1, borderColor: 'divider', minHeight: 40, '& .MuiTab-root': { minHeight: 40, textTransform: 'none' } }}>
        {SECTIONS.map((item) => <Tab key={item.value} value={item.value} label={item.label} />)}
      </Tabs>
      {section === 'users' && <UsersSection />}
      {section === 'rights' && <RightsSection />}
      {section === 'properties' && <PropertiesSection />}
      {section === 'audit' && <AuditSection />}
      {section === 'jobs' && <JobsSection />}
    </Page>
  );
}
