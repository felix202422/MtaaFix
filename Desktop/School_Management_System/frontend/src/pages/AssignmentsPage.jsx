import { useState, useEffect, useCallback } from 'react';
import {
  Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, MenuItem, IconButton, Pagination, Box, Typography,
} from '@mui/material';
import { Plus, Edit, Trash2, Upload, Download } from 'lucide-react';
import { toast } from 'react-toastify';
import dayjs from 'dayjs';
import { assignmentAPI, classAPI, subjectAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

const STATUS_COLORS = {
  PUBLISHED: 'primary',
  DRAFT: 'default',
  CLOSED: 'error',
  GRADED: 'success',
};

const emptyForm = {
  title: '',
  description: '',
  classRoomId: '',
  subjectId: '',
  maxMarks: 100,
  dueDate: dayjs().add(7, 'day').format('YYYY-MM-DDTHH:mm'),
};

export default function AssignmentsPage() {
  const [assignments, setAssignments] = useState([]);
  const [classes, setClasses] = useState([]);
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
      const res = await assignmentAPI.getAll({ page: p, size: 10, sortBy: 'dueDate', sortDir: 'desc' });
      setAssignments(res.data.data.content || res.data.data);
      setTotalPages(res.data.data.totalPages || 1);
    } catch {
      toast.error('Failed to load assignments');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { load(page); }, [load, page]);

  useEffect(() => {
    classAPI.getAll().then((res) => setClasses(res.data.data || [])).catch(() => {});
    subjectAPI.getAll({ size: 100 }).then((res) => {
      const d = res.data.data;
      setSubjects(Array.isArray(d) ? d : d.content || []);
    }).catch(() => {});
  }, []);

  const openCreate = () => { setForm(emptyForm); setEditingId(null); setDialogOpen(true); };
  const openEdit = (a) => {
    setForm({
      title: a.title || '',
      description: a.description || '',
      classRoomId: a.classRoom?.id || '',
      subjectId: a.subject?.id || '',
      maxMarks: a.maxMarks ?? 100,
      dueDate: a.dueDate ? dayjs(a.dueDate).format('YYYY-MM-DDTHH:mm') : '',
    });
    setEditingId(a.id);
    setDialogOpen(true);
  };

  const handleSubmit = async () => {
    const payload = {
      ...form,
      classRoom: form.classRoomId ? { id: Number(form.classRoomId) } : null,
      subject: form.subjectId ? { id: Number(form.subjectId) } : null,
    };
    try {
      if (editingId) {
        await assignmentAPI.update(editingId, payload);
        toast.success('Assignment updated');
      } else {
        await assignmentAPI.create(payload);
        toast.success('Assignment created');
      }
      setDialogOpen(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this assignment?')) return;
    try {
      await assignmentAPI.delete(id);
      toast.success('Assignment deleted');
      load();
    } catch {
      toast.error('Delete failed');
    }
  };

  return (
    <Layout title="Assignments">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Create, publish and grade coursework. Submissions can be graded per student.
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button variant="outlined" startIcon={<Download size={18} />}
            onClick={() => window.open(`${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/reports/students/export?format=csv`, '_blank')}>
            Export
          </Button>
          <Button variant="contained" startIcon={<Plus size={18} />} onClick={openCreate}>
            New Assignment
          </Button>
        </Box>
      </Card>

      <Card>
        {loading ? <TableSkeleton /> : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Title</TableCell>
                    <TableCell>Class</TableCell>
                    <TableCell>Subject</TableCell>
                    <TableCell>Due Date</TableCell>
                    <TableCell>Max Marks</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {assignments.map((a) => (
                    <TableRow key={a.id} hover>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>{a.title}</Typography>
                        {a.description && (
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {a.description}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>{a.classRoom?.name || '-'}</TableCell>
                      <TableCell>{a.subject?.name || '-'}</TableCell>
                      <TableCell>{a.dueDate ? dayjs(a.dueDate).format('DD MMM YYYY, HH:mm') : '-'}</TableCell>
                      <TableCell>{a.maxMarks ?? '-'}</TableCell>
                      <TableCell>
                        <Chip label={a.status || 'PUBLISHED'} size="small" color={STATUS_COLORS[a.status] || 'default'} />
                      </TableCell>
                      <TableCell align="right">
                        <IconButton size="small" onClick={() => openEdit(a)}><Edit size={16} /></IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDelete(a.id)}><Trash2 size={16} /></IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                  {assignments.length === 0 && (
                    <TableRow><TableCell colSpan={7} align="center">No assignments found</TableCell></TableRow>
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
        <DialogTitle>{editingId ? 'Edit Assignment' : 'New Assignment'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField label="Title" value={form.title} required
            onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <TextField label="Description" value={form.description} multiline rows={3}
            onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <TextField select label="Class" value={form.classRoomId}
            onChange={(e) => setForm({ ...form, classRoomId: e.target.value })}>
            {classes.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
          </TextField>
          <TextField select label="Subject" value={form.subjectId}
            onChange={(e) => setForm({ ...form, subjectId: e.target.value })}>
            {subjects.map((s) => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
          </TextField>
          <TextField label="Max Marks" type="number" value={form.maxMarks}
            onChange={(e) => setForm({ ...form, maxMarks: e.target.value })} />
          <TextField label="Due Date" type="datetime-local" value={form.dueDate}
            onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Save</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}
