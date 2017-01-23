import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#2e6b3f', dark: '#1f4d2c' },
    secondary: { main: '#c77700' },
    background: { default: '#f6f7f5' },
  },
  shape: { borderRadius: 6 },
  typography: { fontFamily: 'Roboto, "Segoe UI", Helvetica, Arial, sans-serif', fontSize: 13 },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
  },
});
