import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, List, ListItem, ListItemText,
} from '@mui/material';
import { useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { formatKwh } from '../../components/format';

type Stay = Schemas['StayDTO'];
type Session = Schemas['SupplySessionDTO'];

/** The job that closes the supply sessions of checked-out stays (CheckoutSessionCloseDelayInMinutes later). */
export const CHECKOUT_CLOSE_JOB = 'closeSessionsOnCheckoutJob';

/**
 * Check-out of a stay. The open supply sessions of the stay are closed by the session close job after the check-out,
 * so the dialog names them first and afterwards offers to run the job now (with CONFIG_EDIT).
 */
export function CheckOutDialog({ stay, openSessions, onClose }: {
  stay: Stay | null; openSessions: Session[]; onClose: () => void;
}) {
  const { hasRight } = useAuth();
  const [checkedOut, setCheckedOut] = useState<{ stay: Stay; sessions: Session[] } | null>(null);
  const [jobRun, setJobRun] = useState(false);

  const checkOut = useApiMutation('/api/console/guest/checkOut', {
    invalidates: ['/api/console/guest/', '/api/console/supply/', '/api/console/dashboard/'],
    success: 'Guest checked out',
    onSuccess: (response) => setCheckedOut({ stay: response.stay ?? stay ?? {}, sessions: openSessions }),
  });
  const runJob = useApiMutation('/api/console/job/runJobNow', {
    invalidates: ['/api/console/supply/', '/api/console/guest/', '/api/console/dashboard/'],
    success: 'Session close job started',
    onSuccess: () => setJobRun(true),
  });

  const close = () => {
    setCheckedOut(null);
    setJobRun(false);
    onClose();
  };

  return (
    <Dialog open={stay !== null} onClose={close} fullWidth maxWidth="sm" aria-labelledby="checkout-title">
      <DialogTitle id="checkout-title">Check out {stay?.guestName}</DialogTitle>
      <DialogContent>
        {!checkedOut ? (
          <>
            <DialogContentText>
              Pitch {stay?.pitchNo} at {stay?.campsiteName}, arrived {stay?.arrivalDate}, departure {stay?.departureDate}.
              The check-out ends the validity of card {stay?.cardNumber} and closes the open supply sessions of the stay.
            </DialogContentText>
            {openSessions.length > 0 ? (
              <Alert severity="warning" sx={{ mt: 2 }} data-testid="checkout-open-sessions">
                {openSessions.length} open supply session{openSessions.length > 1 ? 's' : ''} will be closed:
                <List dense disablePadding>
                  {openSessions.map((session) => (
                    <ListItem key={session.id} disableGutters>
                      <ListItemText primary={`Socket ${session.socketName ?? session.socketCode}, since ${session.start?.substring(0, 16).replace('T', ' ')}`}
                        secondary={`${formatKwh(session.kwh)} so far`} />
                    </ListItem>
                  ))}
                </List>
              </Alert>
            ) : (
              <Alert severity="info" sx={{ mt: 2 }}>The stay has no open supply session.</Alert>
            )}
          </>
        ) : (
          <Alert severity="success" data-testid="checkout-result">
            {checkedOut.stay.guestName} checked out{checkedOut.stay.checkOutDate ? ` at ${checkedOut.stay.checkOutDate.substring(0, 16).replace('T', ' ')}` : ''}.{' '}
            {checkedOut.sessions.length > 0
              ? `${checkedOut.sessions.length} open supply session${checkedOut.sessions.length > 1 ? 's are' : ' is'} closed by the session close job at its next run${jobRun ? ' (started now).' : '.'}`
              : 'No supply session had to be closed.'}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        {!checkedOut ? (
          <>
            <Button onClick={close}>Cancel</Button>
            <Button variant="contained" color="warning" disabled={checkOut.isPending}
              onClick={() => checkOut.mutate({ stayId: stay?.id })} data-testid="checkout-confirm">
              Check out
            </Button>
          </>
        ) : (
          <>
            {checkedOut.sessions.length > 0 && hasRight(Right.CONFIG_EDIT) && (
              <Button disabled={runJob.isPending || jobRun} onClick={() => runJob.mutate({ jobName: CHECKOUT_CLOSE_JOB })}
                data-testid="checkout-run-job">
                Run session close job now
              </Button>
            )}
            <Button variant="contained" onClick={close}>Close</Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}
