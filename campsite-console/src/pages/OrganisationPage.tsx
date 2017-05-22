import { Alert, Chip, MenuItem, Stack, Table, TableBody, TableCell, TableRow, TextField, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import { useQueries } from '@tanstack/react-query';
import type { EChartsOption } from 'echarts';
import { useMemo, useState } from 'react';
import { post, type Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { ActionButton, NOT_OFFERED } from '../components/ActionButton';
import { Chart } from '../components/Chart';
import { FormDialog } from '../components/FormDialog';
import { enumLabel } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';

type GroupType = 'ISSUER' | 'SITE' | 'STAFF';
type TreeNode = Schemas['GroupTreeNodeDTO'];
type Group = Schemas['GroupDTO'];
type Attributes = Schemas['GroupAttributesDTO'];

const GROUP_TYPES: { value: GroupType; label: string }[] = [
  { value: 'ISSUER', label: 'Card issuers' },
  { value: 'SITE', label: 'Campsites' },
  { value: 'STAFF', label: 'Staff' },
];

const AFFILIATE_PARENTS = ['NETWORK ACCESS', 'HOUSE CARDS ONLY'];

interface EChartsTreeNode {
  name: string;
  value: number;
  children: EChartsTreeNode[];
  itemStyle?: { color: string; borderColor: string };
}

/** Group tree of the backend as data of an ECharts tree series; the selected group is highlighted. */
/** The group with the longest parent line (the last of equals). */
export function deepestGroup(groups: Group[]): Group | undefined {
  const byId = new Map(groups.map((group) => [group.id, group]));
  const depth = (group: Group): number => {
    let level = 0;
    for (let parent = byId.get(group.parentId); parent && level < 50; parent = byId.get(parent.parentId)) level += 1;
    return level;
  };
  return groups.reduce<Group | undefined>((best, group) => (!best || depth(group) >= depth(best) ? group : best), undefined);
}

/** Number of leaves of a tree: the height the tree diagram needs. */
export function leafCount(node: TreeNode | undefined): number {
  if (!node) return 0;
  const children = node.children ?? [];
  return children.length === 0 ? 1 : children.reduce((sum, child) => sum + leafCount(child), 0);
}

export function toTreeData(node: TreeNode | undefined, selectedId?: number): EChartsTreeNode[] {
  if (!node) return [];
  const convert = (current: TreeNode): EChartsTreeNode => ({
    name: current.displayName || current.name || String(current.id),
    value: current.id as number,
    children: (current.children ?? []).map(convert),
    itemStyle: current.id === selectedId ? { color: '#c77700', borderColor: '#c77700' } : undefined,
  });
  return [convert(node)];
}

/** Access mode of an affiliate campsite follows from its parent group (null: not an affiliate campsite). */
export function affiliateAccessMode(group: Group | undefined, groups: Group[]): string | null {
  if (!group || group.type !== 'SITE') return null;
  const parent = groups.find((candidate) => candidate.id === group.parentId);
  const prefix = AFFILIATE_PARENTS.find((name) => parent?.name?.startsWith(name));
  return prefix ? prefix.replace(/ /g, '_') : null;
}

const ATTRIBUTE_LABELS: [keyof Attributes, string][] = [
  ['type', 'Type'],
  ['market', 'Market'],
  ['siteTenantName', 'Campsite tenant'],
  ['issuerTenantName', 'Issuer tenant'],
  ['exportCampsiteName', 'Export campsite name'],
  ['localList', 'Local card list on pedestals'],
  ['communicationLog', 'Communication log'],
  ['welcomeCardAdoption', 'Welcome card adoption'],
  ['supplierStock', 'Supplier stock'],
  ['accessMode', 'Affiliate access mode'],
];

function attributeValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'yes' : 'no';
  return String(value);
}

/**
 * Tab 3: ISSUER / SITE / STAFF group trees, the acceptance agreements between card issuers and campsites as a graph,
 * and the effective (inherited) attribute values of a group.
 */
export default function OrganisationPage() {
  const [groupType, setGroupType] = useState<GroupType>('SITE');
  const [selectedId, setSelectedId] = useState<number | undefined>();
  const [creating, setCreating] = useState(false);
  const [switching, setSwitching] = useState(false);
  const [issuerId, setIssuerId] = useState<number | undefined>();
  const [validity, setValidity] = useState<boolean | null>(null);

  const tree = useApiQuery('/api/console/group/getGroupTree', { groupType });
  const groups = useApiQuery('/api/console/group/findManageableGroups', { groupType });
  const issuers = useApiQuery('/api/console/group/findManageableGroups', { groupType: 'ISSUER' });
  const sites = useApiQuery('/api/console/group/findManageableGroups', { groupType: 'SITE' });
  const groupList = groups.data?.groups ?? [];
  const issuerList = issuers.data?.groups ?? [];
  const siteList = sites.data?.groups ?? [];

  // without a choice the deepest group is shown: it inherits the most attributes
  const selected = groupList.find((group) => group.id === selectedId) ?? deepestGroup(groupList);
  const attributes = useApiQuery('/api/console/group/getGroupAttributes', { groupId: selected?.id },
    { enabled: selected?.id !== undefined });

  const agreements = useQueries({
    queries: issuerList.map((issuer) => ({
      queryKey: ['/api/console/group/findAcceptanceAgreements', { issuerGroupId: issuer.id }],
      queryFn: () => post('/api/console/group/findAcceptanceAgreements', { issuerGroupId: issuer.id }),
    })),
  });
  const agreementList = agreements.map((query) => query.data?.agreement).filter(Boolean) as Schemas['AcceptanceAgreementDTO'][];
  const currentIssuer = issuerId ?? issuerList.find((issuer) => issuer.parentId)?.id ?? issuerList[0]?.id;
  const currentAgreement = agreementList.find((agreement) => agreement.issuerGroupId === currentIssuer);

  const invalidates = ['/api/console/group/'];
  const createGroup = useApiMutation('/api/console/group/createGroup', {
    invalidates, success: (r) => `Group ${r.group?.name ?? ''} created`, onSuccess: () => setCreating(false),
  });
  const addAgreement = useApiMutation('/api/console/group/addAcceptanceAgreement', { invalidates, success: 'Agreement added' });
  const removeAgreement = useApiMutation('/api/console/group/removeAcceptanceAgreement', { invalidates, success: 'Agreement removed' });
  const switchMode = useApiMutation('/api/console/group/switchAccessMode', {
    invalidates, success: (r) => `Access mode is now ${enumLabel(r.accessMode)}`, onSuccess: () => setSwitching(false),
  });
  const checkValidity = useApiMutation('/api/console/group/checkGroupValidity', {
    success: (r) => (r.valid ? 'The group tree is valid' : 'The group tree has errors'),
    onSuccess: (r) => setValidity(Boolean(r.valid)),
  });

  const accessMode = affiliateAccessMode(selected, groupList);

  const graphOption = useMemo<EChartsOption>(() => {
    type GraphNode = { id: string; name: string; category: number; x: number; y: number; symbolSize: number;
      label: { position: 'left' | 'right' }; value?: [number, number] };
    const issuerNodes: GraphNode[] = [];
    const siteNodes = new Map<string, GraphNode>();
    const links: { source: string; target: string; lineStyle?: { type: 'dashed' } }[] = [];
    const addSite = (site: Group, category: number) => {
      if (!siteNodes.has(`s${site.id}`)) {
        siteNodes.set(`s${site.id}`, { id: `s${site.id}`, name: site.displayName || site.name || '', category, x: 330,
          y: 0, symbolSize: 22, label: { position: 'right' } });
      }
    };
    issuerList.filter((issuer) => issuer.parentId).forEach((issuer) => {
      issuerNodes.push({ id: `i${issuer.id}`, name: issuer.displayName || issuer.name || '', category: 0, x: 0, y: 0,
        symbolSize: 26, label: { position: 'left' } });
    });
    siteList.filter((site) => site.parentId).forEach((site) => addSite(site, 1));
    agreementList.forEach((agreement) => {
      (agreement.agreements ?? []).forEach((site) => {
        addSite(site, 2);
        links.push({ source: `i${agreement.issuerGroupId}`, target: `s${site.id}`,
          lineStyle: agreement.inherited ? { type: 'dashed' as const } : undefined });
      });
    });
    const known = new Set(issuerNodes.map((node) => node.id));
    const shownLinks = links.filter((link) => known.has(link.source));
    const sitesOrdered = [...siteNodes.values()];
    // issuers in a column on the left, campsites on the right, both spread over the height (cartesian: round nodes)
    issuerNodes.forEach((node, index) => { node.value = [0, (index + 0.5) / issuerNodes.length]; });
    sitesOrdered.forEach((node, index) => { node.value = [1, (index + 0.5) / sitesOrdered.length]; });
    issuerNodes.forEach((node) => { node.symbolSize = 18; });
    sitesOrdered.forEach((node) => { node.symbolSize = 14; });
    return {
      tooltip: {},
      legend: [{ data: ['Card issuer', 'Campsite', 'Campsite of another market'], bottom: 0 }],
      grid: { left: 210, right: 230, top: 15, bottom: 40 },
      xAxis: { type: 'value', show: false, min: -0.02, max: 1.02 },
      yAxis: { type: 'value', show: false, min: 0, max: 1, inverse: true },
      dataZoom: [{ type: 'inside', xAxisIndex: 0, filterMode: 'none' }, { type: 'inside', yAxisIndex: 0, filterMode: 'none' }],
      series: [{
        type: 'graph',
        coordinateSystem: 'cartesian2d',
        label: { show: true, fontSize: 11 },
        emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: 9,
        categories: [
          { name: 'Card issuer', itemStyle: { color: '#2e6b3f' } },
          { name: 'Campsite', itemStyle: { color: '#1976d2' } },
          { name: 'Campsite of another market', itemStyle: { color: '#9e9e9e' } },
        ],
        data: [...issuerNodes, ...sitesOrdered],
        links: shownLinks,
        lineStyle: { color: '#7a9a84', width: 1.2, curveness: 0.1, opacity: 0.7 },
      }],
    };
  }, [issuerList, siteList, agreementList]);

  const acceptedIds = new Set(currentAgreement?.acceptingSiteGroupIds ?? []);
  const graphNodes = (graphOption.series as { data?: { category: number }[] }[] | undefined)?.[0]?.data ?? [];
  const graphHeight = Math.min(620, Math.max(300,
    22 * Math.max(graphNodes.filter((node) => node.category === 0).length, graphNodes.filter((node) => node.category !== 0).length) + 80));

  return (
    <Page
      title="Organisation"
      toolbar={(
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          <ToggleButtonGroup
            size="small"
            exclusive
            value={groupType}
            onChange={(_, value: GroupType | null) => { if (value) { setGroupType(value); setSelectedId(undefined); } }}
            aria-label="Group type"
          >
            {GROUP_TYPES.map((type) => (
              <ToggleButton key={type.value} value={type.value}>{type.label}</ToggleButton>
            ))}
          </ToggleButtonGroup>
          <ActionButton label="Create group" variant="contained" onClick={() => setCreating(true)} testId="create-group" />
          <ActionButton label="Run validity check" onClick={() => checkValidity.mutate({})} busy={checkValidity.isPending}
            testId="check-validity" />
        </Stack>
      )}
    >
      {validity !== null && (
        <Alert severity={validity ? 'success' : 'error'} onClose={() => setValidity(null)} data-testid="validity-result">
          {validity ? 'Group validity check passed: every group has a valid parent, type and unique name.'
            : 'Group validity check failed: see the server log for the groups in error.'}
        </Alert>
      )}
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} alignItems="flex-start">
        <Stack spacing={2} sx={{ flex: 3, minWidth: 0, width: '100%' }}>
          <Section title={`${GROUP_TYPES.find((type) => type.value === groupType)?.label} tree`} testId="group-tree">
            <QueryState isLoading={tree.isLoading} error={tree.error}>
              <Chart
                height={Math.min(600, Math.max(280, 46 * leafCount(tree.data?.tree) + 40))}
                testId="group-tree-chart"
                onEvents={{ click: (params) => setSelectedId((params as { data?: { value?: number } }).data?.value) }}
                option={{
                  tooltip: { trigger: 'item', triggerOn: 'mousemove' },
                  series: [{
                    type: 'tree',
                    data: toTreeData(tree.data?.tree, selected?.id),
                    top: 15, left: 30, bottom: 15, right: 200,
                    symbolSize: 12,
                    roam: true,
                    initialTreeDepth: 3,
                    expandAndCollapse: true,
                    animationDurationUpdate: 300,
                    itemStyle: { color: '#2e6b3f', borderColor: '#2e6b3f' },
                    label: { position: 'right', verticalAlign: 'middle', align: 'left', fontSize: 12, distance: 6,
                      backgroundColor: 'rgba(255,255,255,0.9)', padding: [1, 3] },
                    leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
                    lineStyle: { color: '#9bb5a2', width: 2 },
                  }],
                }}
              />
            </QueryState>
          </Section>
          <Section title="Acceptance agreements: card issuers accepted by campsites" testId="agreement-graph">
            <Typography variant="caption" color="text.secondary">
              Scroll to zoom, drag to move; hover a node to see its agreements. Dashed: inherited from the parent issuer.
            </Typography>
            <Chart height={graphHeight} testId="agreement-graph-chart" option={graphOption} />
          </Section>
        </Stack>
        <Stack spacing={2} sx={{ flex: 2, minWidth: 0, width: '100%' }}>
          <Section
            title="Effective attributes"
            flex={2}
            testId="group-attributes"
            actions={(
              <TextField
                select size="small" label="Group" value={selected?.id ?? ''} sx={{ minWidth: 220 }}
                onChange={(e) => setSelectedId(Number(e.target.value))}
                data-testid="group-select"
              >
                {groupList.map((group) => (
                  <MenuItem key={group.id} value={group.id}>{group.displayName || group.name}</MenuItem>
                ))}
              </TextField>
            )}
          >
            <QueryState isLoading={attributes.isLoading} error={attributes.error}>
              <Table size="small">
                <TableBody>
                  <TableRow>
                    <TableCell sx={{ color: 'text.secondary' }}>Group name</TableCell>
                    <TableCell><code>{selected?.name}</code></TableCell>
                  </TableRow>
                  {ATTRIBUTE_LABELS.map(([key, label]) => (
                    <TableRow key={key}>
                      <TableCell sx={{ color: 'text.secondary', width: '50%' }}>{label}</TableCell>
                      <TableCell>{attributeValue(key === 'accessMode' ? attributes.data?.attributes?.accessMode ?? accessMode
                        : attributes.data?.attributes?.[key])}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </QueryState>
            <Stack direction="row" spacing={1} sx={{ mt: 1.5 }} flexWrap="wrap" useFlexGap>
              <ActionButton label="Move group" onClick={() => undefined} disabledReason={NOT_OFFERED} />
              <ActionButton label="Edit attributes" onClick={() => undefined} disabledReason={NOT_OFFERED} />
              <ActionButton
                label="Switch access mode"
                onClick={() => setSwitching(true)}
                disabledReason={accessMode ? null
                  : 'Only affiliate campsites (below Network access or House cards only) have an access mode'}
                testId="switch-access-mode"
              />
            </Stack>
          </Section>
          <Section
            title="Agreements of an issuer"
            flex={2}
            testId="issuer-agreements"
            actions={(
              <TextField
                select size="small" label="Card issuer" value={currentIssuer ?? ''} sx={{ minWidth: 220 }}
                onChange={(e) => setIssuerId(Number(e.target.value))}
                data-testid="issuer-select"
              >
                {issuerList.map((issuer) => (
                  <MenuItem key={issuer.id} value={issuer.id}>{issuer.displayName || issuer.name}</MenuItem>
                ))}
              </TextField>
            )}
          >
            {currentAgreement?.inherited && (
              <Chip size="small" label="inherited from a parent group" sx={{ mb: 1 }} />
            )}
            <Table size="small">
              <TableBody>
                {[...new Map([...siteList.filter((site) => site.parentId), ...(currentAgreement?.agreements ?? [])]
                  .map((site) => [site.id, site])).values()].map((site) => {
                  const accepted = acceptedIds.has(site.id as number);
                  return (
                    <TableRow key={site.id}>
                      <TableCell>{site.displayName || site.name}</TableCell>
                      <TableCell>{accepted ? <Chip size="small" color="success" label="accepts" />
                        : <Chip size="small" label="no agreement" />}</TableCell>
                      <TableCell align="right">
                        {accepted ? (
                          <ActionButton label="Remove" color="error" testId={`remove-agreement-${site.id}`}
                            confirm={`Campsite ${site.displayName || site.name} will no longer accept these cards.`}
                            onClick={() => removeAgreement.mutate({ issuerGroupId: currentIssuer, siteGroupId: site.id })} />
                        ) : (
                          <ActionButton label="Add" testId={`add-agreement-${site.id}`}
                            onClick={() => addAgreement.mutate({ issuerGroupId: currentIssuer, siteGroupId: site.id })} />
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Section>
        </Stack>
      </Stack>
      <FormDialog
        open={creating}
        title={`Create ${GROUP_TYPES.find((type) => type.value === groupType)?.label.toLowerCase()} group`}
        description="The name carries the meaning of the group (market prefix, access mode); it must be unique."
        fields={[
          { name: 'parentGroupId', label: 'Parent group', type: 'select', required: true,
            options: groupList.map((group) => ({ value: group.id as number, label: group.displayName || group.name || '' })) },
          { name: 'name', label: 'Name', required: true, helperText: 'Upper case, e.g. HR CAMP OLIVE GROVE' },
          { name: 'displayName', label: 'Display name', required: true },
        ]}
        initialValues={{ parentGroupId: selected?.id }}
        submitLabel="Create"
        busy={createGroup.isPending}
        onClose={() => setCreating(false)}
        onSubmit={(values) => createGroup.mutate({
          parentGroupId: Number(values.parentGroupId), name: String(values.name), displayName: String(values.displayName),
        })}
      />
      <FormDialog
        open={switching}
        title="Switch affiliate access mode"
        description={`${selected?.displayName ?? ''}: network access opens the pedestals to network cards, house cards only restricts them to the campsite's own cards.`}
        fields={[
          { name: 'accessMode', label: 'Access mode', type: 'select', required: true,
            options: [{ value: 'NETWORK_ACCESS', label: 'Network access' }, { value: 'HOUSE_CARDS_ONLY', label: 'House cards only' }] },
          { name: 'listedInApp', label: 'Pedestals listed in the app (and remote controlled)', type: 'checkbox' },
        ]}
        initialValues={{ accessMode: accessMode === 'NETWORK_ACCESS' ? 'HOUSE_CARDS_ONLY' : 'NETWORK_ACCESS', listedInApp: true }}
        submitLabel="Switch"
        busy={switchMode.isPending}
        onClose={() => setSwitching(false)}
        onSubmit={(values) => switchMode.mutate({
          affiliateGroupId: selected?.id, accessMode: String(values.accessMode), listedInApp: Boolean(values.listedInApp),
        })}
      />
    </Page>
  );
}
