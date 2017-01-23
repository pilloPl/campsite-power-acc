import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Tooltip, type ButtonProps } from '@mui/material';
import { useState, type ReactNode } from 'react';

/**
 * Button of a state-changing action. With `confirm` it asks first; with `disabledReason` it is shown disabled and
 * the reason is the tooltip (actions the backend does not offer, or the user may not do).
 */
export function ActionButton({
  label, onClick, confirm, disabledReason, busy, icon, variant = 'outlined', color = 'primary', size = 'small', testId,
}: {
  label: string;
  onClick: () => void;
  confirm?: ReactNode;
  disabledReason?: string | false | null;
  busy?: boolean;
  icon?: ReactNode;
  variant?: ButtonProps['variant'];
  color?: ButtonProps['color'];
  size?: ButtonProps['size'];
  testId?: string;
}) {
  const [asking, setAsking] = useState(false);
  const button = (
    <Button
      variant={variant}
      color={color}
      size={size}
      startIcon={icon}
      disabled={Boolean(disabledReason) || busy}
      onClick={() => (confirm ? setAsking(true) : onClick())}
      data-testid={testId}
    >
      {label}
    </Button>
  );
  return (
    <>
      {disabledReason ? (
        <Tooltip title={disabledReason}>
          <span>{button}</span>
        </Tooltip>
      ) : (
        button
      )}
      {confirm && (
        <Dialog open={asking} onClose={() => setAsking(false)}>
          <DialogTitle>{label}</DialogTitle>
          <DialogContent>
            <DialogContentText>{confirm}</DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAsking(false)}>Cancel</Button>
            <Button
              variant="contained"
              color={color}
              onClick={() => {
                setAsking(false);
                onClick();
              }}
            >
              Confirm
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </>
  );
}

/** Reason shown on actions the backend has no operation for. */
export const NOT_OFFERED = 'Not offered by the backend yet';
