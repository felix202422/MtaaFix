import { useState, useEffect } from 'react';
import {
  Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, TextField, Box, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  Stack, IconButton, Typography,
} from '@mui/material';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { hostelAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

const emptyForm = { name: '', description: '', capacity: 50 };

export default function HostelPage() {
  const [hostels, setHostels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [expanded, setExpanded] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [roomsLoading, setRoomsLoading] = useState(false);

  useEffect(() => { loadHostels(); }, []);

  const loadHostels = async () => {
    try {
      const res = await hostelAPI.getHostels({ page: 0, size: 50 });
      setHostels(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const loadRooms = async (hostelId) => {
    setRoomsLoading(true);
    try {
      const res = await hostelAPI.getRoomsByHostel(hostelId);
      setRooms(res.data.data);
    } catch (err) { console.error(err); }
    setRoomsLoading(false);
  };

  const openCreate = () => { setEditing(null); setForm(emptyForm); setOpen(true); };
  const openEdit = (hostel) => {
    setEditing(hostel);
    setForm({ name: hostel.name, description: hostel.description || '', capacity: hostel.capacity || 50 });
    setOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editing) await hostelAPI.updateHostel(editing.id, form);
      else await hostelAPI.createHostel(form);
      setOpen(false);
      loadHostels();
    } catch (err) { console.error(err); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this hostel?')) return;
    try { await hostelAPI.deleteHostel(id); loadHostels(); } catch (err) { console.error(err); }
  };

  const toggleExpand = (hostel) => {
    if (expanded === hostel.id) { setExpanded(null); return; }
    setExpanded(hostel.id);
    loadRooms(hostel.id);
  };

  return (
    <Layout title="Hostel">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<Plus size={18} />} onClick={openCreate}>Add Hostel</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Capacity</TableCell>
                  <TableCell>Rooms</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {hostels.map((hostel) => (
                  <TableRow key={hostel.id} hover onClick={() => toggleExpand(hostel)} sx={{ cursor: 'pointer' }}>
                    <TableCell><strong>{hostel.name}</strong></TableCell>
                    <TableCell>{hostel.description || '-'}</TableCell>
                    <TableCell>{hostel.capacity ?? '-'}</TableCell>
                    <TableCell>
                      {expanded === hostel.id && (roomsLoading ? <Chip label="Loading..." size="small" /> : (
                        <Stack direction="row" spacing={1} flexWrap="wrap">
                          {rooms.length === 0 && <Typography variant="caption" color="text.secondary">No rooms</Typography>}
                          {rooms.map((room) => (
                            <Chip key={room.id} size="small" label={`${room.roomNumber} (${room.occupied}/${room.capacity})`} color={room.occupied >= room.capacity ? 'error' : 'default'} />
                          ))}
                        </Stack>
                      ))}
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5}>
                        <IconButton onClick={(e) => { e.stopPropagation(); openEdit(hostel); }}><Edit size={16} /></IconButton>
                        <IconButton onClick={(e) => { e.stopPropagation(); handleDelete(hostel.id); }}><Trash2 size={16} /></IconButton>
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
        <DialogTitle>{editing ? 'Edit Hostel' : 'Add Hostel'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, pt: 2 }}>
            <TextField label="Name" fullWidth required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Capacity" type="number" fullWidth value={form.capacity} onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })} />
            <TextField label="Description" fullWidth multiline rows={3} sx={{ gridColumn: '1 / -1' }} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
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
