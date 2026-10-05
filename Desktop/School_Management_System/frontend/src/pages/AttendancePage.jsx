import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Button, Box } from '@mui/material';
import Layout from '../components/layout/Layout';
import { attendanceAPI } from '../services/api';

export default function AttendancePage() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(false);
  }, []);

  const statusColors = { PRESENT: 'success', ABSENT: 'error', SICK: 'warning', LATE: 'info', EXCUSED: 'default' };

  return (
    <Layout title="Attendance">
      <Card sx={{ p: 3 }}>
        {loading ? null : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Student</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Class</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {records.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} align="center">No attendance records found. Select a class and date to mark attendance.</TableCell>
                  </TableRow>
                )}
                {records.map((r) => (
                  <TableRow key={r.id} hover>
                    <TableCell>{r.student?.user?.firstName} {r.student?.user?.lastName}</TableCell>
                    <TableCell>{r.date}</TableCell>
                    <TableCell>{r.classRoom?.name}</TableCell>
                    <TableCell><Chip label={r.status} color={statusColors[r.status] || 'default'} size="small" /></TableCell>
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
