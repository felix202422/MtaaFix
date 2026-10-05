import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Button, Tabs, Tab } from '@mui/material';
import { Plus } from 'lucide-react';
import { feeAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function FeesPage() {
  const [tab, setTab] = useState(0);
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (tab === 0) {
      feeAPI.getInvoices({ page: 0, size: 20 }).then((res) => { setInvoices(res.data.data.content); setLoading(false); }).catch(() => setLoading(false));
    }
  }, [tab]);

  return (
    <Layout title="Fees Management">
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Invoices" />
        <Tab label="Payments" />
        <Tab label="Fee Structure" />
      </Tabs>
      {tab === 0 && (
        <>
          <Card sx={{ p: 2, mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Button variant="contained" startIcon={<Plus size={18} />}>Create Invoice</Button>
          </Card>
          <Card>
            {loading ? <TableSkeleton /> : (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Invoice #</TableCell>
                      <TableCell>Student</TableCell>
                      <TableCell>Total</TableCell>
                      <TableCell>Paid</TableCell>
                      <TableCell>Due Date</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {invoices.map((inv) => (
                      <TableRow key={inv.id} hover>
                        <TableCell>{inv.invoiceNumber}</TableCell>
                        <TableCell>{inv.student?.user?.firstName} {inv.student?.user?.lastName}</TableCell>
                        <TableCell>${inv.totalAmount}</TableCell>
                        <TableCell>${inv.paidAmount}</TableCell>
                        <TableCell>{inv.dueDate}</TableCell>
                        <TableCell><Chip label={inv.status} color={inv.status === 'PAID' ? 'success' : inv.status === 'PENDING' ? 'warning' : 'info'} size="small" /></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Card>
        </>
      )}
    </Layout>
  );
}
