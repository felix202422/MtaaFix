import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Button } from '@mui/material';
import { Plus } from 'lucide-react';
import { examAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function ExaminationsPage() {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    examAPI.getAll({ page: 0, size: 20 }).then((res) => { setExams(res.data.data.content); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  return (
    <Layout title="Examinations">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<Plus size={18} />}>Create Exam</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Term</TableCell>
                  <TableCell>Start Date</TableCell>
                  <TableCell>End Date</TableCell>
                  <TableCell>Max Marks</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {exams.map((e) => (
                  <TableRow key={e.id} hover>
                    <TableCell>{e.name}</TableCell>
                    <TableCell><Chip label={e.type} size="small" color="primary" variant="outlined" /></TableCell>
                    <TableCell>{e.term?.name}</TableCell>
                    <TableCell>{e.startDate}</TableCell>
                    <TableCell>{e.endDate}</TableCell>
                    <TableCell>{e.maxMarks}</TableCell>
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
