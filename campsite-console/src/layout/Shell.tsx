import { AppBar, Box, Button, Chip, Stack, Tab, Tabs, Toolbar, Typography } from '@mui/material';
import BoltIcon from '@mui/icons-material/Bolt';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { visibleTabs } from '../tabs';
import { enumLabel } from '../components/format';

const NAV_WIDTH = 200;

/** Application bar with the logged-in user and, on the left, the tabs the user's rights allow. */
export function Shell() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const tabs = visibleTabs(user?.rights);
  const current = location.pathname.split('/')[1] ?? '';
  const selected = tabs.some((tab) => tab.path === current) ? current : false;
  const scope = (user?.scopeGroups ?? []).map((group) => group.displayName || group.name).filter(Boolean).join(', ');

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="fixed" elevation={1} sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar variant="dense" sx={{ gap: 1.5 }}>
          <BoltIcon />
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Campsite Power <Typography component="span" variant="body2" sx={{ opacity: 0.8 }}>operations console</Typography>
          </Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <Box sx={{ textAlign: 'right' }}>
              <Typography variant="body2" data-testid="current-user" sx={{ lineHeight: 1.2 }}>
                {user?.name || user?.username}
              </Typography>
              {scope && (
                <Typography variant="caption" sx={{ opacity: 0.8, lineHeight: 1.1 }}>
                  {scope}
                </Typography>
              )}
            </Box>
            {(user?.roles ?? []).map((role) => (
              <Chip key={role} label={enumLabel(role)} size="small" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'inherit' }} />
            ))}
            <Button color="inherit" size="small" onClick={async () => { await logout(); navigate('/login'); }}>
              Log out
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{
          position: 'fixed', top: 48, bottom: 0, left: 0, width: NAV_WIDTH, bgcolor: 'background.paper',
          borderRight: 1, borderColor: 'divider', overflowY: 'auto',
        }}
      >
        <Tabs
          value={selected}
          orientation="vertical"
          variant="scrollable"
          aria-label="Console tabs"
          sx={{
            '& .MuiTab-root': {
              minHeight: 40, py: 0.5, px: 2, alignItems: 'flex-start', textTransform: 'none', fontSize: 14,
              textAlign: 'left',
            },
            '& .Mui-selected': { bgcolor: 'action.selected', fontWeight: 600 },
          }}
        >
          {tabs.map((tab) => (
            <Tab
              key={tab.path}
              value={tab.path}
              label={<span><Box component="span" sx={{ color: 'text.disabled', mr: 1 }}>{tab.no}</Box>{tab.label}</span>}
              aria-label={tab.label}
              component={Link}
              to={`/${tab.path}`}
            />
          ))}
        </Tabs>
      </Box>
      <Box component="main" sx={{ ml: `${NAV_WIDTH}px`, pt: '48px' }}>
        <Outlet />
      </Box>
    </Box>
  );
}
