import { Box, CircularProgress } from '@mui/material';
import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { LoginPage } from './auth/LoginPage';
import { Shell } from './layout/Shell';
import { PAGES } from './pages';
import { visibleTabs } from './tabs';

function TabRoute() {
  const { tab } = useParams();
  const { user } = useAuth();
  const allowed = visibleTabs(user?.rights);
  const definition = allowed.find((candidate) => candidate.path === tab);
  if (!definition) return <Navigate to={allowed.length > 0 ? `/${allowed[0].path}` : '/login'} replace />;
  const Page = PAGES[definition.path];
  return <Page />;
}

function Home() {
  const { user } = useAuth();
  const allowed = visibleTabs(user?.rights);
  if (allowed.length === 0) return <Box sx={{ p: 4 }}>No console tab is available for your roles.</Box>;
  return <Navigate to={`/${allowed[0].path}`} replace />;
}

/** Routes: the login page without a login, otherwise the tabs of the user's rights. */
export function App() {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 20 }}>
        <CircularProgress />
      </Box>
    );
  }
  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }
  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route element={<Shell />}>
        <Route index element={<Home />} />
        <Route path=":tab" element={<TabRoute />} />
      </Route>
    </Routes>
  );
}
