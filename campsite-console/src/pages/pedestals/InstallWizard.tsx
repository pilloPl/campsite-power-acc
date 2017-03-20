import {
  Alert, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, MenuItem, Radio, Stack,
  Step, StepLabel, Stepper, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import type { FormValues } from '../../components/FormDialog';
import { formatDateTime } from '../../components/format';
import { LOCATION_FIELDS, PEDESTAL_READS, toLocation } from './PedestalDetail';

type StockEntry = Schemas['SupplierStockEntryDTO'];

export const ACCESS_MODES = [
  { value: 'HOUSE_CARDS_ONLY', label: 'House cards only' },
  { value: 'NETWORK_ACCESS', label: 'Network access (network and partner cards)' },
];

const STEPS = ['Pedestal from stock', 'Owner and location', 'Confirm'];

/**
 * Wizard of the installation of a pedestal from the supplier stock: choose the pedestal, give the affiliate owner and
 * the location, confirm (commissioning/commissionPedestal).
 */
export function InstallWizard({ open, stock, onClose }: { open: boolean; stock: StockEntry[]; onClose: () => void }) {
  const { user } = useAuth();
  const [step, setStep] = useState(0);
  const [pedestalCode, setPedestalCode] = useState('');
  const [owner, setOwner] = useState('');
  const [accessMode, setAccessMode] = useState('HOUSE_CARDS_ONLY');
  const [listedInApp, setListedInApp] = useState(true);
  const [location, setLocation] = useState<FormValues>({});

  useEffect(() => {
    if (open) {
      setStep(0);
      setPedestalCode(stock[0]?.pedestalCode ?? '');
      setOwner(user?.username ?? '');
      setAccessMode('HOUSE_CARDS_ONLY');
      setListedInApp(true);
      setLocation({});
    }
  }, [open, stock, user]);

  const commission = useApiMutation('/api/console/commissioning/commissionPedestal', {
    invalidates: PEDESTAL_READS,
    success: (r) => `Pedestal ${r.pedestalCode ?? pedestalCode} installed`,
    onSuccess: onClose,
  });

  const locationMissing = LOCATION_FIELDS.some((field) => field.required && !location[field.name]);
  const canNext = step === 0 ? pedestalCode.trim() !== '' : step === 1 ? owner.trim() !== '' && !locationMissing : true;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth aria-labelledby="install-title">
      <DialogTitle id="install-title">Install a pedestal from the supplier stock</DialogTitle>
      <DialogContent>
        <Stepper activeStep={step} sx={{ mb: 3, mt: 1 }}>
          {STEPS.map((label) => <Step key={label}><StepLabel>{label}</StepLabel></Step>)}
        </Stepper>
        {step === 0 && (
          <Stack spacing={2}>
            {stock.length === 0 ? (
              <Alert severity="info">
                The supplier stock of your scope is empty. Enter the code of a delivered pedestal to install it.
              </Alert>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell />
                    <TableCell>Pedestal</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Sockets</TableCell>
                    <TableCell>Serial number</TableCell>
                    <TableCell>Last boot</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {stock.map((entry) => (
                    <TableRow key={entry.id} hover onClick={() => setPedestalCode(entry.pedestalCode ?? '')} sx={{ cursor: 'pointer' }}>
                      <TableCell padding="checkbox"><Radio size="small" checked={entry.pedestalCode === pedestalCode} /></TableCell>
                      <TableCell>{entry.pedestalCode}</TableCell>
                      <TableCell>{entry.pedestalType}</TableCell>
                      <TableCell>{entry.numberOfSockets}</TableCell>
                      <TableCell>{entry.serialNumber}</TableCell>
                      <TableCell>{formatDateTime(entry.lastBoot)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            <TextField label="Pedestal code" size="small" value={pedestalCode} onChange={(e) => setPedestalCode(e.target.value)} required />
          </Stack>
        )}
        {step === 1 && (
          <Stack spacing={2}>
            <TextField label="Affiliate owner (username)" size="small" value={owner} onChange={(e) => setOwner(e.target.value)} required />
            <TextField select label="Access mode" size="small" value={accessMode} onChange={(e) => setAccessMode(e.target.value)}>
              {ACCESS_MODES.map((mode) => <MenuItem key={mode.value} value={mode.value}>{mode.label}</MenuItem>)}
            </TextField>
            <FormControlLabel control={<Checkbox checked={listedInApp} onChange={(e) => setListedInApp(e.target.checked)} />}
              label="Listed in the app" />
            <Stack direction="row" flexWrap="wrap" useFlexGap spacing={2}>
              {LOCATION_FIELDS.filter((field) => field.type !== 'textarea').map((field) => (
                <TextField key={field.name} label={field.label} size="small" required={field.required}
                  type={field.type === 'number' ? 'number' : 'text'} sx={{ flex: '1 1 200px' }}
                  value={location[field.name] === undefined ? '' : String(location[field.name])}
                  onChange={(e) => setLocation((old) => ({
                    ...old, [field.name]: field.type === 'number' ? (e.target.value === '' ? undefined : Number(e.target.value)) : e.target.value,
                  }))} />
              ))}
            </Stack>
          </Stack>
        )}
        {step === 2 && (
          <Stack spacing={1}>
            <Typography>Pedestal <b>{pedestalCode}</b> is installed for <b>{owner}</b> ({ACCESS_MODES.find((m) => m.value === accessMode)?.label}).</Typography>
            <Typography>Location: {[location.name, location.street, location.houseNumber, location.zipcode, location.city, location.countryIso].filter(Boolean).join(', ')}</Typography>
            <Typography>{listedInApp ? 'The pedestal is listed in the app.' : 'The pedestal is not listed in the app.'}</Typography>
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button disabled={step === 0} onClick={() => setStep(step - 1)}>Back</Button>
        {step < 2 ? (
          <Button variant="contained" disabled={!canNext} onClick={() => setStep(step + 1)}>Next</Button>
        ) : (
          <Button variant="contained" disabled={commission.isPending} onClick={() => commission.mutate({
            pedestalCode: pedestalCode.trim(), ownerUsername: owner.trim(), location: toLocation(location), listedInApp, accessMode,
          })}>Install</Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
