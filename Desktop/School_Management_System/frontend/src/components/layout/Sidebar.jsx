import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Drawer, List, ListItemButton, ListItemIcon, ListItemText, Box, Typography, Divider,
} from '@mui/material';
import {
  LayoutDashboard, Users, School, Book, CalendarDays, Wallet, BookOpen, Package,
  Bus, Hotel, CalendarCheck2, Settings, LogOut, Library, ClipboardList, Clock,
} from 'lucide-react';
import { SIDEBAR_WIDTH } from '../../utils/constants';
import { useAuth } from '../../context/AuthContext';
import { navItemsForRole } from '../../utils/permissions';

const ICONS = {
  dashboard: <LayoutDashboard size={20} />,
  users: <Users size={20} />,
  school: <School size={20} />,
  book: <Book size={20} />,
  calendar: <CalendarDays size={20} />,
  exam: <Library size={20} />,
  clipboard: <ClipboardList size={20} />,
  clock: <Clock size={20} />,
  library: <BookOpen size={20} />,
  wallet: <Wallet size={20} />,
  bookOpen: <BookOpen size={20} />,
  package: <Package size={20} />,
  bus: <Bus size={20} />,
  hotel: <Hotel size={20} />,
  calendarCheck: <CalendarCheck2 size={20} />,
  settings: <Settings size={20} />,
};

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const items = navItemsForRole(user?.role);

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: SIDEBAR_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: SIDEBAR_WIDTH,
          boxSizing: 'border-box',
          bgcolor: '#0F172A',
          color: '#fff',
          borderRight: 'none',
        },
      }}
    >
      <Box sx={{ p: 2.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <School size={28} color="#2563EB" />
        <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1.1rem' }}>SMS</Typography>
      </Box>
      <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />
      <List sx={{ px: 1, mt: 1 }}>
        {items.map((item) => (
          <ListItemButton
            key={item.path}
            onClick={() => navigate(item.path)}
            selected={location.pathname === item.path}
            sx={{
              borderRadius: 2, mb: 0.5, color: '#94A3B8',
              '&.Mui-selected': { bgcolor: '#2563EB', color: '#fff', '&:hover': { bgcolor: '#1D4ED8' } },
              '&:hover': { bgcolor: 'rgba(255,255,255,0.08)' },
            }}
          >
            <ListItemIcon sx={{ minWidth: 36, color: 'inherit' }}>{ICONS[item.icon] || ICONS.dashboard}</ListItemIcon>
            <ListItemText primary={item.label} slotProps={{ primary: { fontSize: '0.9rem', fontWeight: 500 } }} />
          </ListItemButton>
        ))}
      </List>
      <Box sx={{ flexGrow: 1 }} />
      <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />
      <Box sx={{ px: 2, py: 1 }}>
        <Typography variant="caption" sx={{ color: '#64748B', textTransform: 'uppercase', letterSpacing: 1 }}>
          {user?.role || 'Guest'}
        </Typography>
      </Box>
      <List sx={{ px: 1, mb: 1 }}>
        <ListItemButton onClick={logout} sx={{ borderRadius: 2, color: '#94A3B8' }}>
          <ListItemIcon sx={{ minWidth: 36, color: 'inherit' }}><LogOut size={20} /></ListItemIcon>
          <ListItemText primary="Logout" slotProps={{ primary: { fontSize: '0.9rem' } }} />
        </ListItemButton>
      </List>
    </Drawer>
  );
}
