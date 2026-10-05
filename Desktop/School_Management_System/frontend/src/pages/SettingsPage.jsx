import { Card, Typography, Box, Grid, TextField, Button, Divider } from '@mui/material';
import Layout from '../components/layout/Layout';

export default function SettingsPage() {
  return (
    <Layout title="Settings">
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>School Information</Typography>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField label="School Name" defaultValue="Bright Future Academy" fullWidth />
              <TextField label="Address" defaultValue="123 Education Lane" fullWidth />
              <TextField label="Phone" defaultValue="+1-555-0123" fullWidth />
              <TextField label="Email" defaultValue="info@brightfuture.edu" fullWidth />
              <Button variant="contained" sx={{ mt: 1 }}>Save Changes</Button>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>Academic Settings</Typography>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField label="Current Academic Year" defaultValue="2026/2027" fullWidth />
              <TextField label="Current Term" defaultValue="Term 1" fullWidth />
              <Button variant="contained">Update</Button>
            </Box>
          </Card>
        </Grid>
      </Grid>
    </Layout>
  );
}
