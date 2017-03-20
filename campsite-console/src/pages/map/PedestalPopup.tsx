import { Box, Button, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { ActionButton } from '../../components/ActionButton';
import { FormDialog } from '../../components/FormDialog';
import { formatDateTime } from '../../components/format';
import { StatusChip } from '../../components/StatusChip';

type MapPedestal = Schemas['MapPedestalDTO'];
type MapSocket = Schemas['MapSocketDTO'];

const REFRESH = ['/api/console/map/', '/api/console/supply/', '/api/console/dashboard/', '/api/console/pedestal/'];
const FAULT_STATUSES = ['FAULTED', 'BREAKER_TRIPPED'];

function commandMessage(response: Schemas['PedestalCommandResponse'], done: string): string {
  const command = response.command;
  if (command && command.accepted === false) return command.message || 'The pedestal did not accept the command';
  return command?.message ? `${done}: ${command.message}` : done;
}

/** Popup of a pedestal on the map: sockets with status and the remote commands of the service and reception. */
export function PedestalPopup({ pedestal, onOpen }: { pedestal: MapPedestal; onOpen: (code: string) => void }) {
  const { hasRight } = useAuth();
  const canControl = hasRight(Right.SOCKET_REMOTE_CONTROL);
  const canReset = hasRight(Right.PEDESTAL_EDIT);
  const [starting, setStarting] = useState<MapSocket | null>(null);
  const queryClient = useQueryClient();
  // the pedestal reports the end or start of the supply shortly after it accepted the command
  const refreshLater = () => {
    window.setTimeout(() => {
      void queryClient.invalidateQueries({
        predicate: (query) => REFRESH.some((prefix) => String(query.queryKey[0]).startsWith(prefix)),
      });
    }, 2500);
  };

  const stopSupply = useApiMutation('/api/console/supply/stopSupply', {
    invalidates: REFRESH, success: (r) => commandMessage(r, 'Stop accepted by the pedestal'), onSuccess: refreshLater,
  });
  const startSupply = useApiMutation('/api/console/supply/startSupply', {
    invalidates: REFRESH, success: (r) => commandMessage(r, 'Start accepted by the pedestal'),
    onSuccess: () => { setStarting(null); refreshLater(); },
  });
  const resetFault = useApiMutation('/api/console/monitoring/resetFault', {
    invalidates: REFRESH, success: (r) => commandMessage(r, 'Fault reset'),
  });

  const noRemote = !pedestal.remoteControl ? 'The pedestal has no remote control' : null;
  const controlReason = !canControl ? 'Requires right SOCKET_REMOTE_CONTROL' : noRemote;

  return (
    <Box data-testid="pedestal-popup" sx={{ minWidth: 380 }}>
      <Stack direction="row" alignItems="baseline" spacing={1} sx={{ mb: 0.5 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>{pedestal.code}</Typography>
        <Typography variant="caption" color="text.secondary">
          {pedestal.pedestalType} · link {pedestal.linkVersion}
        </Typography>
      </Stack>
      <Typography variant="caption" color={pedestal.timeout ? 'error' : 'text.secondary'} component="div">
        {pedestal.timeout ? 'Heartbeat timeout – ' : 'Last message '}{formatDateTime(pedestal.lastIncoming)}
      </Typography>
      <Table size="small" sx={{ my: 1, '& td, & th': { px: 0.75, py: 0.5 } }}>
        <TableHead>
          <TableRow>
            <TableCell>Socket</TableCell>
            <TableCell>Pitch</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Command</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {(pedestal.sockets ?? []).map((socket) => {
            const fault = FAULT_STATUSES.includes(socket.status ?? '');
            return (
              <TableRow key={socket.id} data-testid={`popup-socket-${socket.id}`}>
                <TableCell>
                  {socket.socketNo}
                  {socket.sessionMaxCurrentA ? <Typography variant="caption" color="text.secondary"> {socket.sessionMaxCurrentA} A</Typography> : null}
                </TableCell>
                <TableCell>{socket.pitchNo}</TableCell>
                <TableCell><StatusChip status={socket.status} /></TableCell>
                <TableCell align="right">
                  {socket.activeSessionId ? (
                    <ActionButton label="Stop" color="error" size="small" busy={stopSupply.isPending}
                      confirm={`Stop the supply on socket ${socket.socketNo} (pitch ${socket.pitchNo ?? '–'})?`}
                      onClick={() => stopSupply.mutate({ sessionId: socket.activeSessionId })}
                      disabledReason={controlReason} testId={`popup-stop-${socket.id}`} />
                  ) : fault ? (
                    <ActionButton label="Reset RCD" color="warning" size="small" busy={resetFault.isPending}
                      onClick={() => resetFault.mutate({ pedestalCode: pedestal.code, socketNo: socket.socketNo, socketId: socket.id })}
                      disabledReason={!canReset && 'Requires right PEDESTAL_EDIT'} testId={`popup-reset-${socket.id}`} />
                  ) : (
                    <ActionButton label="Start" size="small" onClick={() => setStarting(socket)}
                      disabledReason={controlReason ?? (socket.status !== 'AVAILABLE' && socket.status !== 'OCCUPIED'
                        ? 'The socket is not available' : null)}
                      testId={`popup-start-${socket.id}`} />
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
      <Button size="small" variant="contained" onClick={() => onOpen(pedestal.code ?? '')} data-testid="popup-open-pedestal">
        Open pedestal
      </Button>
      <FormDialog
        open={starting !== null}
        title={`Start supply on socket ${starting?.socketNo ?? ''} of ${pedestal.code}`}
        description="The supply is started for the card; the pedestal checks the card like at the reader."
        fields={[{ name: 'cardRfid', label: 'Card RFID', required: true }]}
        submitLabel="Start supply"
        busy={startSupply.isPending}
        onClose={() => setStarting(null)}
        onSubmit={(values) => startSupply.mutate({ socketId: starting?.id, cardRfid: String(values.cardRfid) })}
      />
    </Box>
  );
}
