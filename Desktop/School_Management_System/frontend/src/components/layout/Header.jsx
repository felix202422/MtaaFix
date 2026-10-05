import { AppBar, Toolbar, Typography, Box, IconButton, Avatar, Badge, Tooltip } from '@mui/material';
import { Bell, Sun, Moon, Menu } from 'lucide-react';
import { useThemeMode } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';

export default function Header({ title }) {
  const { mode, toggleTheme } = useThemeMode();
  const { user } = useAuth();

  return (
    <AppBar position="sticky" elevation={0} sx={{ bgcolor: 'transparent', backdropFilter: 'blur(10px)', borderBottom: '1px solid', borderColor: 'divider' }}>
      <Toolbar>
        <Typography variant="h6" sx={{ fontWeight: 600, flexGrow: 1 }}>{title || 'Dashboard'}</Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Tooltip title={mode === 'light' ? 'Dark mode' : 'Light mode'}>
            <IconButton onClick={toggleTheme} size="small">
              {mode === 'light' ? <Moon size={20} /> : <Sun size={20} />}
            </IconButton>
          </Tooltip>
          <IconButton size="small">
            <Badge badgeContent={3} color="error">
              <Bell size={20} />
            </Badge>
          </IconButton>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, ml: 1 }}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: '#2563EB', fontSize: '0.9rem' }}>
              {user?.fullName?.charAt(0) || 'U'}
            </Avatar>
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Typography variant="body2" sx={{ fontWeight: 600, lineHeight: 1.2 }}>{user?.fullName || 'User'}</Typography>
              <Typography variant="caption" color="text.secondary">{user?.role || ''}</Typography>
            </Box>
          </Box>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
