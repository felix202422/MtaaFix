import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button } from '@mui/material';
import { Plus } from 'lucide-react';
import { classAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function ClassesPage() {
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    classAPI.getAll().then((res) => { setClasses(res.data.data); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  return (
    <Layout title="Classes">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<Plus size={18} />}>Add Class</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Section</TableCell>
                  <TableCell>Department</TableCell>
                  <TableCell>Room</TableCell>
                  <TableCell>Capacity</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {classes.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell>{c.name}</TableCell>
                    <TableCell>{c.section}</TableCell>
                    <TableCell>{c.department?.name}</TableCell>
                    <TableCell>{c.roomNumber}</TableCell>
                    <TableCell>{c.capacity}</TableCell>
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
