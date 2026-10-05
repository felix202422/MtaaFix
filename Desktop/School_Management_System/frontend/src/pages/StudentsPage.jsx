import { useState, useEffect } from 'react';
import { Box, Card, TextField, Button, IconButton, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';
import { Plus, Edit, Trash2, Search } from 'lucide-react';
import { studentAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function StudentsPage() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    loadStudents();
  }, [page]);

  const loadStudents = async () => {
    try {
      const res = await studentAPI.getAll({ page, size: 20 });
      setStudents(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const handleSearch = async () => {
    if (!search) { loadStudents(); return; }
    setLoading(true);
    try {
      const res = await studentAPI.search({ q: search, page: 0, size: 20 });
      setStudents(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  return (
    <Layout title="Students">
      <Card sx={{ p: 2, mb: 3, display: 'flex', gap: 2, alignItems: 'center' }}>
        <TextField size="small" placeholder="Search by name or admission number..." value={search} onChange={(e) => setSearch(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSearch()} sx={{ flexGrow: 1 }} />
        <Button variant="contained" startIcon={<Search size={18} />} onClick={handleSearch}>Search</Button>
        <Button variant="contained" startIcon={<Plus size={18} />}>Add Student</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Admission #</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Gender</TableCell>
                  <TableCell>Class</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {students.map((s) => (
                  <TableRow key={s.id} hover>
                    <TableCell>{s.admissionNumber}</TableCell>
                    <TableCell>{s.user?.firstName} {s.user?.lastName}</TableCell>
                    <TableCell>{s.gender}</TableCell>
                    <TableCell>{s.currentClass?.name}</TableCell>
                    <TableCell><Chip label={s.academicStatus} color={s.academicStatus === 'ACTIVE' ? 'success' : 'default'} size="small" /></TableCell>
                    <TableCell>
                      <IconButton size="small"><Edit size={16} /></IconButton>
                      <IconButton size="small" color="error"><Trash2 size={16} /></IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>
    </Layout>
  );
}
