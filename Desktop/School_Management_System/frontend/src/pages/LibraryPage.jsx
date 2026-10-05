import { useState, useEffect } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, TextField, Box, Chip } from '@mui/material';
import { Plus, Search } from 'lucide-react';
import { libraryAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { TableSkeleton } from '../components/common/LoadingSkeleton';

export default function LibraryPage() {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => { loadBooks(); }, []);

  const loadBooks = async () => {
    try {
      const res = await libraryAPI.getBooks({ page: 0, size: 20 });
      setBooks(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const handleSearch = async () => {
    if (!search) { loadBooks(); return; }
    setLoading(true);
    try {
      const res = await libraryAPI.searchBooks({ q: search, page: 0, size: 20 });
      setBooks(res.data.data.content);
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  return (
    <Layout title="Library">
      <Card sx={{ p: 2, mb: 3, display: 'flex', gap: 2 }}>
        <TextField size="small" placeholder="Search books..." value={search} onChange={(e) => setSearch(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSearch()} sx={{ flexGrow: 1 }} />
        <Button variant="contained" startIcon={<Search size={18} />} onClick={handleSearch}>Search</Button>
        <Button variant="contained" startIcon={<Plus size={18} />}>Add Book</Button>
      </Card>
      <Card>
        {loading ? <TableSkeleton /> : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Author</TableCell>
                  <TableCell>ISBN</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Available</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {books.map((b) => (
                  <TableRow key={b.id} hover>
                    <TableCell>{b.title}</TableCell>
                    <TableCell>{b.author}</TableCell>
                    <TableCell>{b.isbn}</TableCell>
                    <TableCell><Chip label={b.category} size="small" /></TableCell>
                    <TableCell>{b.availableQuantity}/{b.quantity}</TableCell>
                    <TableCell><Button size="small">Borrow</Button></TableCell>
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
