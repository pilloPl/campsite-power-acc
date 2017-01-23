import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import BoltIcon from '@mui/icons-material/Bolt';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth } from './AuthContext';

/** Login with username and password; the backend answers the user and keeps the login in the session cookie. */
export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(username.trim(), password);
      navigate('/', { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'grey.100' }}>
      <Card sx={{ width: 380 }} elevation={3}>
        <CardContent>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
            <BoltIcon color="primary" fontSize="large" />
            <Box>
              <Typography variant="h5">Campsite Power</Typography>
              <Typography variant="body2" color="text.secondary">Operations console</Typography>
            </Box>
          </Stack>
          <form onSubmit={submit}>
            <Stack spacing={2}>
              {error && <Alert severity="error" data-testid="login-error">{error}</Alert>}
              <TextField label="Username" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus
                autoComplete="username" required />
              <TextField label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password" required />
              <Button type="submit" variant="contained" size="large" disabled={busy || !username || !password}>
                Log in
              </Button>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
