import {
  Button, Dialog, DialogActions, DialogContent, DialogTitle, Table, TableBody, TableCell, TableRow, TextField,
} from '@mui/material';
import { useEffect, useState } from 'react';
import type { Schemas } from '../../api/client';
import { formatDateTime, formatKwh, formatMoney } from '../../components/format';

type ConsumptionRecord = Schemas['ConsumptionRecordDTO'];

/** Review of a SUSPECT record: the reviewer approves (record is billed) or rejects it with a comment. */
export function ReviewDialog({ record, busy, onDecide, onClose }: {
  record: ConsumptionRecord | null;
  busy?: boolean;
  onDecide: (confirmed: boolean, comment: string) => void;
  onClose: () => void;
}) {
  const [comment, setComment] = useState('');
  useEffect(() => {
    if (record) setComment('');
  }, [record]);

  const rows: [string, string][] = record ? [
    ['Record', record.recordNo ?? String(record.id)],
    ['Suspect because', record.suspectReason ?? ''],
    ['Guest', `${record.guestName ?? ''} (${record.cardNumber ?? record.cardRfid ?? ''})`],
    ['Pedestal / socket', `${record.pedestalCode ?? ''} / ${record.socketCode ?? ''}, pitch ${record.pitchNo ?? ''}`],
    ['Supply', `${formatDateTime(record.plugIn)} - ${formatDateTime(record.plugOut)}, ${record.nights ?? 0} night(s)`],
    ['Consumption', formatKwh(record.kwh)],
    ['Price', `${formatMoney(record.cost, record.currency)} (${record.tariffName ?? ''})`],
  ] : [];

  return (
    <Dialog open={record !== null} onClose={onClose} fullWidth maxWidth="sm" data-testid="review-dialog">
      <DialogTitle>Review SUSPECT record</DialogTitle>
      <DialogContent>
        <Table size="small" sx={{ mb: 2 }}>
          <TableBody>
            {rows.map(([label, value]) => (
              <TableRow key={label}>
                <TableCell sx={{ fontWeight: 600, width: 160 }}>{label}</TableCell>
                <TableCell>{value}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <TextField label="Comment" value={comment} onChange={(e) => setComment(e.target.value)} multiline minRows={2}
          fullWidth size="small" />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button color="error" variant="outlined" disabled={busy} onClick={() => onDecide(false, comment)}>Reject</Button>
        <Button color="success" variant="contained" disabled={busy} onClick={() => onDecide(true, comment)}>Approve</Button>
      </DialogActions>
    </Dialog>
  );
}
