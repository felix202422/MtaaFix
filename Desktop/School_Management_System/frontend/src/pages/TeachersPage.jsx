import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Chip } from '@mui/material';
import { Plus } from 'lucide-react';
import { teacherAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function TeachersPage() {
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    teacherAPI.getAll({ page: 0, size: 50 }).then((res) => { setTeachers(res.data.data.content); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  return (
    <Layout title="Teachers">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<Plus size={18} />}>Add Teacher</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Employee #</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Department</TableCell>
                  <TableCell>Qualification</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {teachers.map((t) => (
                  <TableRow key={t.id} hover>
                    <TableCell>{t.employeeNumber}</TableCell>
                    <TableCell>{t.user?.firstName} {t.user?.lastName}</TableCell>
                    <TableCell>{t.department?.name}</TableCell>
                    <TableCell>{t.qualification}</TableCell>
                    <TableCell><Chip label={t.employmentStatus} color={t.employmentStatus === 'ACTIVE' ? 'success' : 'default'} size="small" /></TableCell>
                    <TableCell><Button size="small">View</Button></TableCell>
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
