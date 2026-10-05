import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Card, TextField, Button, Typography, Alert, Checkbox, FormControlLabel } from '@mui/material';
import { School, LogIn } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await login(username, password, rememberMe);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: '#F8FAFC' }}>
      <Card sx={{ p: 4, maxWidth: 420, width: '100%', mx: 2 }}>
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <School size={48} color="#2563EB" />
          <Typography variant="h5" sx={{ fontWeight: 700, mt: 1 }}>School Management System</Typography>
          <Typography color="text.secondary" variant="body2">Sign in to your account</Typography>
        </Box>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="Username" value={username} onChange={(e) => setUsername(e.target.value)} sx={{ mb: 2 }} required />
          <TextField fullWidth label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} sx={{ mb: 1 }} required />
          <FormControlLabel
            control={<Checkbox checked={rememberMe} onChange={(e) => setRememberMe(e.target.checked)} size="small" />}
            label={<Typography variant="body2" color="text.secondary">Remember me for 30 days</Typography>}
            sx={{ mb: 2 }}
          />
          <Button fullWidth variant="contained" type="submit" size="large" startIcon={<LogIn size={20} />}>
            Sign In
          </Button>
        </form>
      </Card>
    </Box>
  );
}
