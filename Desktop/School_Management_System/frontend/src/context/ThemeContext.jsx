import { createContext, useContext, useState, useMemo } from 'react';
import { createTheme, ThemeProvider as MuiThemeProvider } from '@mui/material/styles';

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const [mode, setMode] = useState(localStorage.getItem('theme') || 'light');

  const theme = useMemo(() => createTheme({
    palette: {
      mode,
      primary: { main: '#2563EB' },
      ...(mode === 'light'
        ? { background: { default: '#F8FAFC', paper: '#FFFFFF' }, text: { primary: '#1E293B' } }
        : { background: { default: '#0F172A', paper: '#1E293B' }, text: { primary: '#F1F5F9' } }),
    },
    typography: { fontFamily: '"Poppins", sans-serif' },
    shape: { borderRadius: 12 },
    components: {
      MuiCard: { styleOverrides: { root: { boxShadow: '0 1px 3px 0 rgb(0 0 0 / 0.1)' } } },
      MuiButton: { styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } } },
    },
  }), [mode]);

  const toggleTheme = () => {
    const newMode = mode === 'light' ? 'dark' : 'light';
    setMode(newMode);
    localStorage.setItem('theme', newMode);
  };

  return (
    <ThemeContext.Provider value={{ mode, toggleTheme }}>
      <MuiThemeProvider theme={theme}>{children}</MuiThemeProvider>
    </ThemeContext.Provider>
  );
}

export const useThemeMode = () => useContext(ThemeContext);
