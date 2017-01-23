import { Card, CardContent, Typography } from '@mui/material';
import type { ReactNode } from 'react';

/** Key figure of the dashboard. */
export function KpiCard({ label, value, hint, tone = 'default', testId }: {
  label: string; value: ReactNode; hint?: string; tone?: 'default' | 'warning' | 'error' | 'success'; testId?: string;
}) {
  const colour = tone === 'default' ? 'text.primary' : `${tone}.main`;
  return (
    <Card variant="outlined" sx={{ minWidth: 120, flex: '1 1 120px' }} data-testid={testId}>
      <CardContent sx={{ p: 1.5, pb: '12px !important' }}>
        <Typography variant="body2" color="text.secondary" gutterBottom noWrap title={label}>
          {label}
        </Typography>
        <Typography variant="h5" component="div" sx={{ color: colour, fontWeight: 500 }}>
          {value}
        </Typography>
        {hint && (
          <Typography variant="caption" color="text.secondary">
            {hint}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
