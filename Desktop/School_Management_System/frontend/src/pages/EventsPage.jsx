import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Chip } from '@mui/material';
import { Plus } from 'lucide-react';
import { eventAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function EventsPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    eventAPI.getAll().then((res) => { setEvents(res.data.data); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  return (
    <Layout title="Events">
      <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<Plus size={18} />}>Create Event</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Start Date</TableCell>
                  <TableCell>Location</TableCell>
                  <TableCell>Public</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {events.map((e) => (
                  <TableRow key={e.id} hover>
                    <TableCell>{e.title}</TableCell>
                    <TableCell><Chip label={e.eventType} size="small" /></TableCell>
                    <TableCell>{new Date(e.startDate).toLocaleDateString()}</TableCell>
                    <TableCell>{e.location}</TableCell>
                    <TableCell><Chip label={e.isPublic ? 'Yes' : 'No'} color={e.isPublic ? 'success' : 'default'} size="small" /></TableCell>
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
