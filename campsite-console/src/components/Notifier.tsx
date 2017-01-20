import { Alert, Snackbar } from '@mui/material';
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type Severity = 'success' | 'error' | 'info' | 'warning';
type Notify = (message: string, severity?: Severity) => void;

const NotifierContext = createContext<Notify>(() => undefined);

/** Shows the outcome of actions (success or the fault message of the backend) as a snackbar. */
export function NotifierProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<{ text: string; severity: Severity; key: number } | null>(null);
  const notify = useCallback<Notify>((text, severity = 'info') => {
    setMessage({ text, severity, key: Date.now() });
  }, []);
  return (
    <NotifierContext.Provider value={notify}>
      {children}
      <Snackbar
        key={message?.key}
        open={message !== null}
        autoHideDuration={6000}
        onClose={() => setMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        {message ? (
          <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)} data-testid="notification">
            {message.text}
          </Alert>
        ) : undefined}
      </Snackbar>
    </NotifierContext.Provider>
  );
}

export function useNotifier(): Notify {
  return useContext(NotifierContext);
}
