import { useState, useEffect, useCallback } from 'react';
import {
  Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, MenuItem, IconButton, Pagination, Box, Typography, FormControl, InputLabel, Select,
} from '@mui/material';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { timetableAPI, classAPI, subjectAPI, teacherAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const DAY_COLORS = {
  MONDAY: 'primary', TUESDAY: 'info', WEDNESDAY: 'success',
  THURSDAY: 'warning', FRIDAY: 'secondary',
};

const emptyForm = {
  classRoomId: '',
  subjectId: '',
  teacherId: '',
  dayOfWeek: 'MONDAY',
  startTime: '08:00',
  endTime: '09:00',
  room: '',
};

export default function TimetablePage() {
  const [entries, setEntries] = useState([]);
  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [classFilter, setClassFilter] = useState('');
  const [dayFilter, setDayFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  const load = useCallback(async (p = page) => {
    setLoading(true);
    try {
      let res;
      if (classFilter) {
        res = await timetableAPI.getByClass(classFilter, dayFilter || undefined);
        setEntries(res.data.data || []);
        setTotalPages(1);
      } else {
        res = await timetableAPI.getAll({ page: p, size: 10 });
        setEntries(res.data.data.content || res.data.data);
        setTotalPages(res.data.data.totalPages || 1);
      }
    } catch {
      toast.error('Failed to load timetable');
    } finally {
      setLoading(false);
    }
  }, [page, classFilter, dayFilter]);

  useEffect(() => { load(page); }, [load]);

  useEffect(() => {
    classAPI.getAll().then((res) => setClasses(res.data.data || [])).catch(() => {});
    subjectAPI.getAll({ size: 100 }).then((res) => {
      const d = res.data.data;
      setSubjects(Array.isArray(d) ? d : d.content || []);
    }).catch(() => {});
    teacherAPI.getAll({ size: 100 }).then((res) => {
      const d = res.data.data;
      setTeachers(Array.isArray(d) ? d : d.content || []);
    }).catch(() => {});
  }, []);

  const openCreate = () => { setForm(emptyForm); setEditingId(null); setDialogOpen(true); };
  const openEdit = (t) => {
    setForm({
      classRoomId: t.classRoom?.id || '',
      subjectId: t.subject?.id || '',
      teacherId: t.teacher?.id || '',
      dayOfWeek: t.dayOfWeek || 'MONDAY',
      startTime: t.startTime || '08:00',
      endTime: t.endTime || '09:00',
      room: t.room || '',
    });
    setEditingId(t.id);
    setDialogOpen(true);
  };

  const handleSubmit = async () => {
    const payload = {
      classRoom: form.classRoomId ? { id: Number(form.classRoomId) } : null,
      subject: form.subjectId ? { id: Number(form.subjectId) } : null,
      teacher: form.teacherId ? { id: Number(form.teacherId) } : null,
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime,
      endTime: form.endTime,
      room: form.room,
    };
    try {
      if (editingId) {
        await timetableAPI.update(editingId, payload);
        toast.success('Timetable entry updated');
      } else {
        await timetableAPI.create(payload);
        toast.success('Timetable entry created');
      }
      setDialogOpen(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this timetable entry?')) return;
    try {
      await timetableAPI.delete(id);
      toast.success('Entry deleted');
      load();
    } catch {
      toast.error('Delete failed');
    }
  };

  return (
    <Layout title="Timetable">
      <Card sx={{ p: 2, mb: 3, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Filter by class</InputLabel>
          <Select label="Filter by class" value={classFilter}
            onChange={(e) => { setClassFilter(e.target.value); setPage(0); }}>
            <MenuItem value="">All classes</MenuItem>
            {classes.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 160 }} disabled={!classFilter}>
          <InputLabel>Day</InputLabel>
          <Select label="Day" value={dayFilter} onChange={(e) => setDayFilter(e.target.value)}>
            <MenuItem value="">All days</MenuItem>
            {DAYS.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
          </Select>
        </FormControl>
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="contained" startIcon={<Plus size={18} />} onClick={openCreate}>
          New Entry
        </Button>
      </Card>

      <Card>
        {loading ? <TableSkeleton /> : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Day</TableCell>
                    <TableCell>Time</TableCell>
                    <TableCell>Class</TableCell>
                    <TableCell>Subject</TableCell>
                    <TableCell>Teacher</TableCell>
                    <TableCell>Room</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {entries.map((t) => (
                    <TableRow key={t.id} hover>
                      <TableCell><Chip label={t.dayOfWeek} size="small" color={DAY_COLORS[t.dayOfWeek] || 'default'} /></TableCell>
                      <TableCell>{t.startTime} – {t.endTime}</TableCell>
                      <TableCell>{t.classRoom?.name || '-'}</TableCell>
                      <TableCell>{t.subject?.name || '-'}</TableCell>
                      <TableCell>{t.teacher?.user?.fullName || '-'}</TableCell>
                      <TableCell>{t.room || '-'}</TableCell>
                      <TableCell align="right">
                        <IconButton size="small" onClick={() => openEdit(t)}><Edit size={16} /></IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDelete(t.id)}><Trash2 size={16} /></IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                  {entries.length === 0 && (
                    <TableRow><TableCell colSpan={7} align="center">No timetable entries found</TableCell></TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
            {!classFilter && totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <Pagination count={totalPages} page={page + 1} onChange={(e, v) => setPage(v - 1)} />
              </Box>
            )}
          </>
        )}
      </Card>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? 'Edit Entry' : 'New Timetable Entry'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField select label="Class" value={form.classRoomId} required
            onChange={(e) => setForm({ ...form, classRoomId: e.target.value })}>
            {classes.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
          </TextField>
          <TextField select label="Subject" value={form.subjectId}
            onChange={(e) => setForm({ ...form, subjectId: e.target.value })}>
            {subjects.map((s) => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
          </TextField>
          <TextField select label="Teacher" value={form.teacherId}
            onChange={(e) => setForm({ ...form, teacherId: e.target.value })}>
            {teachers.map((t) => (
              <MenuItem key={t.id} value={t.id}>
                {t.user?.fullName || t.staffNumber || `Teacher #${t.id}`}
              </MenuItem>
            ))}
          </TextField>
          <TextField select label="Day" value={form.dayOfWeek}
            onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })}>
            {DAYS.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
          </TextField>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Start" type="time" value={form.startTime} fullWidth
              onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
            <TextField label="End" type="time" value={form.endTime} fullWidth
              onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
          </Box>
          <TextField label="Room" value={form.room}
            onChange={(e) => setForm({ ...form, room: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Save</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}
