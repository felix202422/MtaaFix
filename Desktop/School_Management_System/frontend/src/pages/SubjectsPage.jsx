import { useState, useEffect, useCallback } from 'react';
import {
  Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, IconButton, Pagination, Box, Typography,
} from '@mui/material';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { subjectAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

const emptyForm = { name: '', code: '', description: '', credits: 1 };

export default function SubjectsPage() {
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  const load = useCallback(async (p = page) => {
    setLoading(true);
    try {
      const res = await subjectAPI.getAll({ page: p, size: 10 });
      const d = res.data.data;
      if (Array.isArray(d)) {
        setSubjects(d);
        setTotalPages(1);
      } else {
        setSubjects(d.content || []);
        setTotalPages(d.totalPages || 1);
      }
    } catch {
      toast.error('Failed to load subjects');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { load(page); }, [load]);

  const openCreate = () => { setForm(emptyForm); setEditingId(null); setDialogOpen(true); };
  const openEdit = (s) => {
    setForm({
      name: s.name || '',
      code: s.code || '',
      description: s.description || '',
      credits: s.credits ?? 1,
    });
    setEditingId(s.id);
    setDialogOpen(true);
  };

  const handleSubmit = async () => {
    try {
      if (editingId) {
        await subjectAPI.update(editingId, form);
        toast.success('Subject updated');
      } else {
        await subjectAPI.create(form);
        toast.success('Subject created');
      }
      setDialogOpen(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this subject?')) return;
    try {
      await subjectAPI.delete(id);
      toast.success('Subject deleted');
      load();
    } catch {
      toast.error('Delete failed — subject may be in use');
    }
  };

  return (
    <Layout title="Subjects">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Curriculum subjects available across classes and departments.
        </Typography>
        <Button variant="contained" startIcon={<Plus size={18} />} onClick={openCreate}>
          New Subject
        </Button>
      </Card>

      <Card>
        {loading ? <TableSkeleton /> : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Code</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Credits</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {subjects.map((s) => (
                    <TableRow key={s.id} hover>
                      <TableCell><Typography variant="body2" sx={{ fontWeight: 600 }}>{s.code}</Typography></TableCell>
                      <TableCell>{s.name}</TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', maxWidth: 360, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {s.description || '—'}
                        </Typography>
                      </TableCell>
                      <TableCell>{s.credits ?? '—'}</TableCell>
                      <TableCell align="right">
                        <IconButton size="small" onClick={() => openEdit(s)}><Edit size={16} /></IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDelete(s.id)}><Trash2 size={16} /></IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                  {subjects.length === 0 && (
                    <TableRow><TableCell colSpan={5} align="center">No subjects found</TableCell></TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
            {totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <Pagination count={totalPages} page={page + 1} onChange={(e, v) => setPage(v - 1)} />
              </Box>
            )}
          </>
        )}
      </Card>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? 'Edit Subject' : 'New Subject'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField label="Name" value={form.name} required
            onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <TextField label="Code" value={form.code} required
            onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <TextField label="Description" value={form.description} multiline rows={2}
            onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <TextField label="Credits" type="number" value={form.credits}
            onChange={(e) => setForm({ ...form, credits: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Save</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}
