import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OrganisationPage, { affiliateAccessMode, toTreeData } from './OrganisationPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const siteTree = {
  id: 11, name: 'HR SITES', displayName: 'Croatia campsites', type: 'SITE',
  children: [
    { id: 51, name: 'NETWORK ACCESS HR', displayName: 'Network access Croatia', type: 'SITE',
      children: [{ id: 77, name: 'HR OLIVE GROVE', displayName: 'Olive Grove', type: 'SITE', children: [] }] },
    { id: 900000001, name: 'ISTRIA BAY CAMP', displayName: 'Camp Istria Bay', type: 'SITE', children: [] },
  ],
};
const sites = [
  { id: 11, name: 'HR SITES', displayName: 'Croatia campsites', type: 'SITE', parentId: null },
  { id: 51, name: 'NETWORK ACCESS HR', displayName: 'Network access Croatia', type: 'SITE', parentId: 11 },
  { id: 77, name: 'HR OLIVE GROVE', displayName: 'Olive Grove', type: 'SITE', parentId: 51 },
  { id: 900000001, name: 'ISTRIA BAY CAMP', displayName: 'Camp Istria Bay', type: 'SITE', parentId: 11 },
];
const issuers = [
  { id: 1, name: 'HR ISSUERS', displayName: 'Croatia card issuers', type: 'ISSUER', parentId: null },
  { id: 31, name: 'NETWORK CARD', displayName: 'Network card', type: 'ISSUER', parentId: 1 },
];

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/group/getGroupTree': { tree: siteTree },
    '/api/console/group/findManageableGroups': (body: { groupType: string }) => ({ groups: body.groupType === 'ISSUER' ? issuers : sites }),
    '/api/console/group/getGroupAttributes': (body: { groupId: number }) => ({
      attributes: { groupId: body.groupId, type: 'SITE', market: 'HR', siteTenantName: 'Croatia campsites', localList: true },
    }),
    '/api/console/group/findAcceptanceAgreements': (body: { issuerGroupId: number }) => ({
      agreement: body.issuerGroupId === 31
        ? { issuerGroupId: 31, inherited: false, agreements: [{ id: 900000002, name: 'GARDA LAKESIDE', displayName: 'Garda Lakeside Camping', type: 'SITE' }], acceptingSiteGroupIds: [900000002] }
        : { issuerGroupId: 1, inherited: false, agreements: [], acceptingSiteGroupIds: [] },
    }),
    '/api/console/group/addAcceptanceAgreement': { agreement: { issuerGroupId: 31 } },
    '/api/console/group/checkGroupValidity': { valid: true },
  });
}

describe('organisation tab', () => {
  it('turns the group tree into an ECharts tree and finds affiliate campsites by their parent group', () => {
    expect(toTreeData(siteTree as never, 51)[0].children[0]).toMatchObject({
      name: 'Network access Croatia', value: 51, itemStyle: { color: '#c77700' },
      children: [{ name: 'Olive Grove', value: 77 }],
    });
    expect(affiliateAccessMode(sites[2] as never, sites as never)).toBe('NETWORK_ACCESS');
    expect(affiliateAccessMode(sites[3] as never, sites as never)).toBeNull();
  });

  it('shows the tree diagram, the agreement graph and the effective attributes of a group', async () => {
    backend();
    renderWithProviders(<OrganisationPage />);

    await waitFor(() => expect(within(screen.getByTestId('group-tree-chart')).getByTestId('echarts-option'))
      .toHaveTextContent('Camp Istria Bay'));
    await waitFor(() => expect(within(screen.getByTestId('agreement-graph-chart')).getByTestId('echarts-option'))
      .toHaveTextContent('"source":"i31","target":"s900000002"'));
    expect(await within(screen.getByTestId('group-attributes')).findByText('Croatia campsites', { selector: 'td' })).toBeInTheDocument();
  });

  it('adds an acceptance agreement between the issuer and a campsite', async () => {
    const calls = backend();
    renderWithProviders(<OrganisationPage />);

    await userEvent.click(await screen.findByTestId('add-agreement-900000001'));

    await waitFor(() => expect(calls.callsTo('/api/console/group/addAcceptanceAgreement')).toHaveLength(1));
    expect(calls.callsTo('/api/console/group/addAcceptanceAgreement')[0].body).toEqual({ issuerGroupId: 31, siteGroupId: 900000001 });
  });

  it('runs the group validity check and shows its result', async () => {
    const calls = backend();
    renderWithProviders(<OrganisationPage />);

    await userEvent.click(await screen.findByTestId('check-validity'));

    expect(await screen.findByTestId('validity-result')).toHaveTextContent('passed');
    expect(calls.callsTo('/api/console/group/checkGroupValidity')).toHaveLength(1);
  });

  it('offers the access mode switch only for affiliate campsites and shows moving a group as not offered', async () => {
    backend();
    renderWithProviders(<OrganisationPage />);

    expect(await screen.findByTestId('switch-access-mode')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Move group' })).toBeDisabled();
  });
});
