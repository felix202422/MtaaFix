import { useState, useEffect } from 'react';
import {
  Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, TextField, Box, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  Stack, IconButton,
} from '@mui/material';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { transportAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

const emptyForm = { name: '', busNumber: '', driverName: '', driverPhone: '', routeDescription: '', capacity: 30, fare: '' };

export default function TransportPage() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => { loadRoutes(); }, []);

  const loadRoutes = async () => {
    try {
      const res = await transportAPI.getRoutes({ page: 0, size: 50 });
      setRoutes(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const openCreate = () => { setEditing(null); setForm(emptyForm); setOpen(true); };
  const openEdit = (route) => {
    setEditing(route);
    setForm({ name: route.name, busNumber: route.busNumber || '', driverName: route.driverName || '', driverPhone: route.driverPhone || '', routeDescription: route.routeDescription || '', capacity: route.capacity || 30, fare: route.fare || '' });
    setOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editing) await transportAPI.updateRoute(editing.id, form);
      else await transportAPI.createRoute(form);
      setOpen(false);
      loadRoutes();
    } catch (err) { console.error(err); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this route?')) return;
    try { await transportAPI.deleteRoute(id); loadRoutes(); } catch (err) { console.error(err); }
  };

  return (
    <Layout title="Transport">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<Plus size={18} />} onClick={openCreate}>Add Route</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Route Name</TableCell>
                  <TableCell>Bus Number</TableCell>
                  <TableCell>Driver</TableCell>
                  <TableCell>Driver Phone</TableCell>
                  <TableCell>Capacity</TableCell>
                  <TableCell>Fare</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {routes.map((route) => (
                  <TableRow key={route.id} hover>
                    <TableCell><strong>{route.name}</strong></TableCell>
                    <TableCell>{route.busNumber || '-'}</TableCell>
                    <TableCell>{route.driverName || '-'}</TableCell>
                    <TableCell>{route.driverPhone || '-'}</TableCell>
                    <TableCell>{route.capacity ?? '-'}</TableCell>
                    <TableCell>{route.fare ? `$${route.fare}` : '-'}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5}>
                        <IconButton onClick={() => openEdit(route)}><Edit size={16} /></IconButton>
                        <IconButton onClick={() => handleDelete(route.id)}><Trash2 size={16} /></IconButton>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Edit Route' : 'Add Route'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, pt: 2 }}>
            <TextField label="Route Name" fullWidth required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Bus Number" fullWidth value={form.busNumber} onChange={(e) => setForm({ ...form, busNumber: e.target.value })} />
            <TextField label="Driver Name" fullWidth value={form.driverName} onChange={(e) => setForm({ ...form, driverName: e.target.value })} />
            <TextField label="Driver Phone" fullWidth value={form.driverPhone} onChange={(e) => setForm({ ...form, driverPhone: e.target.value })} />
            <TextField label="Capacity" type="number" fullWidth value={form.capacity} onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })} />
            <TextField label="Fare" type="number" fullWidth value={form.fare} onChange={(e) => setForm({ ...form, fare: e.target.value })} />
            <TextField label="Route Description" fullWidth multiline rows={2} sx={{ gridColumn: '1 / -1' }} value={form.routeDescription} onChange={(e) => setForm({ ...form, routeDescription: e.target.value })} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>{editing ? 'Save' : 'Add'}</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}
