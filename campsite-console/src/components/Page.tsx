import { Alert, Box, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { errorMessage } from '../api/client';

/** Frame of a tab: title, optional toolbar (filters, actions) and the content. */
export function Page({ title, toolbar, children }: { title: string; toolbar?: ReactNode; children: ReactNode }) {
  return (
    <Box sx={{ p: 2 }}>
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 2, minHeight: 48 }} flexWrap="wrap" useFlexGap>
        <Typography variant="h5" component="h1" sx={{ flexGrow: 1 }}>
          {title}
        </Typography>
        {toolbar}
      </Stack>
      <Stack spacing={2}>{children}</Stack>
    </Box>
  );
}

/** A titled block of a tab. */
export function Section({ title, actions, children, testId, flex }: {
  title?: string; actions?: ReactNode; children: ReactNode; testId?: string;
  /** Share of the row when sections stand side by side. */
  flex?: number;
}) {
  return (
    <Paper variant="outlined" sx={{ p: 2, flex, minWidth: 0 }} data-testid={testId}>
      {(title || actions) && (
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }} flexWrap="wrap" useFlexGap>
          <Typography variant="subtitle1" component="h2" sx={{ fontWeight: 600, flexGrow: 1 }}>
            {title}
          </Typography>
          {actions}
        </Stack>
      )}
      {children}
    </Paper>
  );
}

/** Loading spinner or the fault of a read; renders the children once the data is there. */
export function QueryState({ isLoading, error, children }: { isLoading: boolean; error: unknown; children: ReactNode }) {
  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress size={32} />
      </Box>
    );
  }
  if (error) return <Alert severity="error">{errorMessage(error)}</Alert>;
  return <>{children}</>;
}
