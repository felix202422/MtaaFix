import { useState, useEffect } from 'react';
import {
  Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, TextField, Box, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  MenuItem, Stack, Typography, IconButton,
} from '@mui/material';
import { Plus, Search, Edit, Trash2 } from 'lucide-react';
import { inventoryAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

const emptyForm = { name: '', description: '', category: '', quantity: 0, unitPrice: '', supplier: '', purchaseDate: '', location: '', status: 'AVAILABLE' };

export default function InventoryPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => { loadItems(); }, []);

  const loadItems = async () => {
    try {
      const res = await inventoryAPI.getAll({ page: 0, size: 50 });
      setItems(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const handleSearch = async () => {
    setLoading(true);
    try {
      const res = search
        ? await inventoryAPI.search({ q: search, page: 0, size: 50 })
        : await inventoryAPI.getAll({ page: 0, size: 50 });
      setItems(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const openCreate = () => { setEditing(null); setForm(emptyForm); setOpen(true); };
  const openEdit = (item) => {
    setEditing(item);
    setForm({ name: item.name, description: item.description || '', category: item.category || '', quantity: item.quantity, unitPrice: item.unitPrice || '', supplier: item.supplier || '', purchaseDate: item.purchaseDate || '', location: item.location || '', status: item.status });
    setOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editing) await inventoryAPI.update(editing.id, form);
      else await inventoryAPI.create(form);
      setOpen(false);
      loadItems();
    } catch (err) { console.error(err); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this inventory item?')) return;
    try { await inventoryAPI.delete(id); loadItems(); } catch (err) { console.error(err); }
  };

  const handleAdjust = async (item, delta) => {
    try { await inventoryAPI.adjustQuantity(item.id, delta); loadItems(); } catch (err) { console.error(err); }
  };

  return (
    <Layout title="Inventory">
      <Card sx={{ p: 2, mb: 3, display: 'flex', gap: 2 }}>
        <TextField size="small" placeholder="Search by name, category or supplier..." value={search} onChange={(e) => setSearch(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSearch()} sx={{ flexGrow: 1 }} />
        <Button variant="outlined" startIcon={<Search size={18} />} onClick={handleSearch}>Search</Button>
        <Button variant="contained" startIcon={<Plus size={18} />} onClick={openCreate}>Add Item</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Quantity</TableCell>
                  <TableCell>Unit Price</TableCell>
                  <TableCell>Supplier</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {items.map((item) => (
                  <TableRow key={item.id} hover>
                    <TableCell>{item.name}</TableCell>
                    <TableCell><Chip label={item.category} size="small" /></TableCell>
                    <TableCell>
                      <Stack direction="row" alignItems="center" spacing={1}>
                        <IconButton size="small" onClick={() => handleAdjust(item, -1)}>-</IconButton>
                        <Typography>{item.quantity}</Typography>
                        <IconButton size="small" onClick={() => handleAdjust(item, 1)}>+</IconButton>
                      </Stack>
                    </TableCell>
                    <TableCell>{item.unitPrice ? `$${item.unitPrice}` : '-'}</TableCell>
                    <TableCell>{item.supplier || '-'}</TableCell>
                    <TableCell><Chip label={item.status} size="small" color={item.status === 'OUT_OF_STOCK' ? 'error' : 'success'} /></TableCell>
                    <TableCell>
                      <IconButton onClick={() => openEdit(item)}><Edit size={16} /></IconButton>
                      <IconButton onClick={() => handleDelete(item.id)}><Trash2 size={16} /></IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Edit Item' : 'Add Item'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, pt: 2 }}>
            <TextField label="Name" fullWidth required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Category" fullWidth value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
            <TextField label="Quantity" type="number" fullWidth value={form.quantity} onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })} />
            <TextField label="Unit Price" type="number" fullWidth value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} />
            <TextField label="Supplier" fullWidth value={form.supplier} onChange={(e) => setForm({ ...form, supplier: e.target.value })} />
            <TextField label="Purchase Date" type="date" fullWidth value={form.purchaseDate} onChange={(e) => setForm({ ...form, purchaseDate: e.target.value })} />
            <TextField label="Location" fullWidth value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
            <TextField label="Status" select fullWidth value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              <MenuItem value="AVAILABLE">Available</MenuItem>
              <MenuItem value="LOW_STOCK">Low Stock</MenuItem>
              <MenuItem value="OUT_OF_STOCK">Out of Stock</MenuItem>
            </TextField>
            <TextField label="Description" fullWidth multiline rows={2} sx={{ gridColumn: '1 / -1' }} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>{editing ? 'Save' : 'Add'}</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}
