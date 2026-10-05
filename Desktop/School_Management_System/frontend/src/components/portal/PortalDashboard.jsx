import { useEffect, useState } from 'react';
import { useLocation, Navigate } from 'react-router-dom';
import { Box, Card, CardContent, Typography, Chip, Divider, Grid, Alert, Skeleton } from '@mui/material';
import { Users, BookOpen, ClipboardList, CalendarClock, TrendingUp, GraduationCap, Baby } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { dashboardAPI } from '../../services/api';
import Layout from '../layout/Layout';

function StatCard({ icon, label, value, color = '#2563EB' }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box sx={{
          width: 48, height: 48, borderRadius: 2, display: 'flex', alignItems: 'center',
          justifyContent: 'center', bgcolor: `${color}1A`, color,
        }}>
          {icon}
        </Box>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>{value ?? '—'}</Typography>
          <Typography variant="body2" color="text.secondary">{label}</Typography>
        </Box>
      </CardContent>
    </Card>
  );
}

function PortalLoading() {
  return (          <Grid container spacing={3}>
            {[1, 2, 3, 4].map((i) => (
              <Grid item xs={12} sm={6} md={3} key={i}>
          <Card><CardContent><Skeleton variant="rectangular" height={64} /></CardContent></Card>
        </Grid>
      ))}
    </Grid>
  );
}

export default function PortalDashboard() {
  const { user } = useAuth();
  const role = user?.role;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const location = useLocation();

  useEffect(() => {
    if (!role) return;
    const fetchers = {
      TEACHER: dashboardAPI.getTeacher,
      STUDENT: dashboardAPI.getStudent,
      PARENT: dashboardAPI.getParent,
    };
    const fetcher = fetchers[role];
    if (!fetcher) { setLoading(false); return; }
    fetcher()
      .then((res) => setData(res.data.data))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [role, location.pathname]);

  if (role === 'SUPER_ADMIN' || role === 'SCHOOL_ADMIN' || !role) {
    return <Navigate to="/" replace />;
  }

  if (loading) return <PortalLoading />;

  const error = data?.error;

  return (
    <Layout title="My Portal">
    <Box>
      {error && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          {error} — showing limited portal view. Contact the school office if this seems wrong.
        </Alert>
      )}

      {role === 'TEACHER' && (
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 0.5 }}>
            Welcome back, {data?.name || 'Teacher'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Employee No: {data?.employeeNumber || '—'} · Your teaching overview
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard icon={<Users size={24} />} label="Classes taught" value={data?.classesCount} />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard icon={<BookOpen size={24} />} label="Subjects" value={data?.subjectsCount} color="#7C3AED" />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard icon={<ClipboardList size={24} />} label="Assignments given" value={data?.assignmentsCount} color="#059669" />
            </Grid>
          </Grid>
        </Box>
      )}

      {role === 'STUDENT' && (
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 0.5 }}>
            Hello, {data?.name || 'Student'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {data?.admissionNumber && <>Admission No: {data.admissionNumber} · </>}
            {data?.className || 'No class assigned'}
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard icon={<TrendingUp size={24} />} label="Attendance rate (30d)" value={data?.attendanceRate != null ? `${data.attendanceRate}%` : null} color="#059669" />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard icon={<ClipboardList size={24} />} label="Assignments due" value={data?.assignmentsDue} color="#DC2626" />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard icon={<GraduationCap size={24} />} label="Recorded results" value={data?.resultsCount} color="#7C3AED" />
            </Grid>
          </Grid>
        </Box>
      )}

      {role === 'PARENT' && (
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 0.5 }}>
            Welcome, {user?.fullName || 'Parent'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Overview of your {data?.children ?? 0} registered {data?.children === 1 ? 'child' : 'children'}
          </Typography>
          {data?.childrenList?.length ? (
            <Grid container spacing={3}>
              {data.childrenList.map((child) => (
                <Grid item xs={12} sm={6} md={4} key={child.id}>
                  <Card>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
                        <Box sx={{
                          width: 40, height: 40, borderRadius: '50%', bgcolor: '#2563EB1A',
                          display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#2563EB',
                        }}>
                          <Baby size={20} />
                        </Box>
                        <Box>
                          <Typography sx={{ fontWeight: 600 }}>{child.name}</Typography>
                          <Typography variant="caption" color="text.secondary">{child.admissionNumber}</Typography>
                        </Box>
                      </Box>
                      <Divider sx={{ my: 1 }} />
                      <Chip label={child.className || 'No class'} size="small" color="primary" variant="outlined" />
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          ) : (
            <Alert severity="info">No children are linked to your account yet.</Alert>
          )}
        </Box>
      )}
    </Box>
    </Layout>
  );
}
