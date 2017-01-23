import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, FormControlLabel, MenuItem,
  Stack, TextField,
} from '@mui/material';
import { useEffect, useState, type ReactNode } from 'react';

export type FieldType = 'text' | 'number' | 'date' | 'datetime' | 'select' | 'checkbox' | 'textarea' | 'password';

export interface FieldSpec {
  name: string;
  label: string;
  type?: FieldType;
  required?: boolean;
  options?: { value: string | number; label: string }[];
  helperText?: string;
}

export type FormValues = Record<string, string | number | boolean | undefined>;

/** Dialog with a simple form; the values are handed to `onSubmit` as typed by the fields. */
export function FormDialog({
  open, title, description, fields, initialValues, submitLabel = 'Save', busy, onSubmit, onClose, children,
}: {
  open: boolean;
  title: string;
  description?: ReactNode;
  fields: FieldSpec[];
  initialValues?: FormValues;
  submitLabel?: string;
  busy?: boolean;
  onSubmit: (values: FormValues) => void;
  onClose: () => void;
  children?: ReactNode;
}) {
  const [values, setValues] = useState<FormValues>(initialValues ?? {});
  useEffect(() => {
    if (open) setValues(initialValues ?? {});
    // the initial values are taken when the dialog opens
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const missing = fields.some((field) => field.required && (values[field.name] === undefined || values[field.name] === ''));
  const set = (name: string, value: string | number | boolean | undefined) => setValues((old) => ({ ...old, [name]: value }));

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="form-dialog-title">
      <DialogTitle id="form-dialog-title">{title}</DialogTitle>
      <DialogContent>
        {description && <DialogContentText sx={{ mb: 2 }}>{description}</DialogContentText>}
        <Stack spacing={2} sx={{ pt: 1 }}>
          {fields.map((field) => {
            const type = field.type ?? 'text';
            const value = values[field.name];
            if (type === 'checkbox') {
              return (
                <FormControlLabel
                  key={field.name}
                  control={<Checkbox checked={Boolean(value)} onChange={(e) => set(field.name, e.target.checked)} />}
                  label={field.label}
                />
              );
            }
            return (
              <TextField
                key={field.name}
                label={field.label}
                required={field.required}
                select={type === 'select'}
                multiline={type === 'textarea'}
                minRows={type === 'textarea' ? 3 : undefined}
                type={type === 'number' ? 'number' : type === 'date' ? 'date' : type === 'datetime' ? 'datetime-local'
                  : type === 'password' ? 'password' : 'text'}
                value={value === undefined ? '' : String(value)}
                helperText={field.helperText}
                onChange={(e) => {
                  const raw = e.target.value;
                  set(field.name, type === 'number' ? (raw === '' ? undefined : Number(raw)) : raw);
                }}
                slotProps={{ inputLabel: { shrink: type === 'date' || type === 'datetime' ? true : undefined } }}
                size="small"
                fullWidth
              >
                {type === 'select' && (field.options ?? []).map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            );
          })}
          {children}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={missing || busy} onClick={() => onSubmit(values)}>
          {submitLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
