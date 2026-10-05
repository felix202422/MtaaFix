import { useState, useEffect } from 'react';
import { Grid, Card, Typography, Box, useTheme } from '@mui/material';
import { Users, School, Wallet, CalendarDays, Book } from 'lucide-react';
import {
  ResponsiveContainer, AreaChart, Area, LineChart, Line, BarChart, Bar,
  PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
} from 'recharts';
import dayjs from 'dayjs';
import { dashboardAPI } from '../services/api';
import Layout from '../components/layout/Layout';
import { StatsCardSkeleton } from '../components/common/LoadingSkeleton';

const CHART_COLORS = ['#2563EB', '#0EA5E9', '#22C55E', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];
const ATTENDANCE_COLORS = { PRESENT: '#22C55E', ABSENT: '#EF4444', LATE: '#F59E0B', SICK: '#0EA5E9', EXCUSED: '#8B5CF6' };
const GENDER_COLORS = { MALE: '#2563EB', FEMALE: '#EC4899', OTHER: '#22C55E' };
const GRADE_ORDER = ['A', 'B+', 'B', 'C+', 'C', 'D+', 'D', 'E'];

const iconMap = {
  totalStudents: <Users size={24} />, activeStudents: <Users size={24} />,
  totalTeachers: <School size={24} />, totalClasses: <Book size={24} />,
  totalPayments: <Wallet size={24} />, upcomingEvents: <CalendarDays size={24} />,
};

function StatCard({ label, value, icon }) {
  return (
    <Card sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
      <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: '#2563EB', color: '#fff', display: 'flex' }}>{icon}</Box>
      <Box>
        <Typography color="text.secondary" variant="body2">{label}</Typography>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>{value?.toLocaleString() || 0}</Typography>
      </Box>
    </Card>
  );
}

function ChartCard({ title, subtitle, children, height = 300 }) {
  return (
    <Card sx={{ p: 2.5, height: '100%', display: 'flex', flexDirection: 'column', borderRadius: '14px' }}>
      <Box sx={{ mb: 2 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>{title}</Typography>
        {subtitle && <Typography variant="caption" color="text.secondary">{subtitle}</Typography>}
      </Box>
      <Box sx={{ flex: 1, minHeight: height }}>
        <ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer>
      </Box>
    </Card>
  );
}

function EmptyChart({ mode }) {
  return (
    <Box sx={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Typography variant="body2" color="text.secondary">No data available</Typography>
    </Box>
  );
}

const compact = (n) =>
  n >= 1_000_000 ? `${(n / 1_000_000).toFixed(1)}M` : n >= 1_000 ? `${(n / 1_000).toFixed(0)}K` : n;

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const theme = useTheme();

  useEffect(() => {
    dashboardAPI.getAdmin().then((res) => { setStats(res.data.data); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const charts = stats?.charts || {};
  const axis = { fontSize: 11, fill: theme.palette.text.secondary };
  const tooltipStyle = {
    borderRadius: 10, border: `1px solid ${theme.palette.divider}`,
    fontFamily: 'inherit', fontSize: 12, bgcolor: theme.palette.background.paper,
  };

  // --- Chart data shaping -------------------------------------------------
  const attendanceTrend = (charts.attendanceTrend || []).map((d) => ({
    ...d, label: dayjs(d.date).format('DD MMM'),
  }));

  const attendanceSplit = charts.attendanceDistribution || [];

  const genderData = charts.genderDistribution || [];

  const monthlyRevenue = charts.monthlyRevenue || [];

  const gradeData = GRADE_ORDER
    .map((g) => (charts.gradeDistribution || []).find((d) => d.grade === g))
    .filter(Boolean);

  const classSizes = (charts.classSizes || []).slice(0, 30);

  const cards = stats ? [
    { label: 'Total Students', value: stats.totalStudents, icon: iconMap.totalStudents },
    { label: 'Active Students', value: stats.activeStudents, icon: iconMap.activeStudents },
    { label: 'Total Teachers', value: stats.totalTeachers, icon: iconMap.totalTeachers },
    { label: 'Total Classes', value: stats.totalClasses, icon: iconMap.totalClasses },
    { label: 'Total Payments', value: stats.totalPayments, icon: iconMap.totalPayments },
    { label: 'Upcoming Events', value: stats.upcomingEvents, icon: iconMap.upcomingEvents },
  ] : [];

  return (
    <Layout title="Dashboard">
      <Grid container spacing={3}>
        {loading ? Array.from({ length: 6 }).map((_, i) => (
          <Grid item xs={12} sm={6} md={4} key={i}><StatsCardSkeleton /></Grid>
        )) : cards.map((card) => (
          <Grid item xs={12} sm={6} md={4} key={card.label}>
            <StatCard {...card} />
          </Grid>
        ))}

        {!loading && (
          <>
            {/* Attendance trend (Area) */}
            <Grid item xs={12} md={8}>
              <ChartCard title="Attendance Trend" subtitle="Daily present rate — last 30 attendance days">
                {attendanceTrend.length ? (
                  <AreaChart data={attendanceTrend} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                    <defs>
                      <linearGradient id="attGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#2563EB" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="#2563EB" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} vertical={false} />
                    <XAxis dataKey="label" tick={axis} tickLine={false} interval="preserveStartEnd" minTickGap={24} />
                    <YAxis tick={axis} tickLine={false} axisLine={false} domain={[0, 100]} unit="%" />
                    <Tooltip contentStyle={tooltipStyle} formatter={(v) => [`${v}%`, 'Present rate']} />
                    <Area type="monotone" dataKey="rate" stroke="#2563EB" strokeWidth={2} fill="url(#attGrad)" />
                  </AreaChart>
                ) : <EmptyChart />}
              </ChartCard>
            </Grid>

            {/* Attendance status split (Pie) */}
            <Grid item xs={12} md={4}>
              <ChartCard title="Attendance Status" subtitle="All recorded attendance">
                {attendanceSplit.length ? (
                  <PieChart>
                    <Pie data={attendanceSplit} dataKey="value" nameKey="name" innerRadius={0} outerRadius="80%" paddingAngle={2}>
                      {attendanceSplit.map((entry) => (
                        <Cell key={entry.name} fill={ATTENDANCE_COLORS[entry.name] || '#94A3B8'} />
                      ))}
                    </Pie>
                    <Tooltip contentStyle={tooltipStyle} />
                    <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                  </PieChart>
                ) : <EmptyChart />}
              </ChartCard>
            </Grid>

            {/* Monthly fee revenue (Bar) */}
            <Grid item xs={12} md={6}>
              <ChartCard title="Fee Collection" subtitle="Payments received per month">
                {monthlyRevenue.length ? (
                  <BarChart data={monthlyRevenue} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} vertical={false} />
                    <XAxis dataKey="month" tick={axis} tickLine={false} />
                    <YAxis tick={axis} tickLine={false} axisLine={false} tickFormatter={compact} />
                    <Tooltip contentStyle={tooltipStyle} formatter={(v) => [Number(v).toLocaleString(), 'Collected']} />
                    <Bar dataKey="amount" fill="#2563EB" radius={[6, 6, 0, 0]} maxBarSize={42} />
                  </BarChart>
                ) : <EmptyChart />}
              </ChartCard>
            </Grid>

            {/* Grade distribution (Bar) */}
            <Grid item xs={12} md={6}>
              <ChartCard title="Exam Grade Distribution" subtitle="All examination results">
                {gradeData.length ? (
                  <BarChart data={gradeData} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} vertical={false} />
                    <XAxis dataKey="grade" tick={axis} tickLine={false} />
                    <YAxis tick={axis} tickLine={false} axisLine={false} tickFormatter={compact} />
                    <Tooltip contentStyle={tooltipStyle} formatter={(v) => [Number(v).toLocaleString(), 'Results']} />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]} maxBarSize={42}>
                      {gradeData.map((entry, i) => <Cell key={entry.grade} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
                    </Bar>
                  </BarChart>
                ) : <EmptyChart />}
              </ChartCard>
            </Grid>

            {/* Students per class (Line) */}
            <Grid item xs={12} md={8}>
              <ChartCard title="Students per Class" subtitle="Enrollment across all classes">
                {classSizes.length ? (
                  <LineChart data={classSizes} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} vertical={false} />
                    <XAxis dataKey="name" tick={axis} tickLine={false} interval={2} />
                    <YAxis tick={axis} tickLine={false} axisLine={false} />
                    <Tooltip contentStyle={tooltipStyle} formatter={(v) => [Number(v).toLocaleString(), 'Students']} />
                    <Line type="monotone" dataKey="students" stroke="#2563EB" strokeWidth={2} dot={{ r: 2.5 }} activeDot={{ r: 4 }} />
                  </LineChart>
                ) : <EmptyChart />}
              </ChartCard>
            </Grid>

            {/* Gender distribution (Donut) */}
            <Grid item xs={12} md={4}>
              <ChartCard title="Students by Gender" subtitle="Total enrollment">
                {genderData.length ? (
                  <PieChart>
                    <Pie data={genderData} dataKey="value" nameKey="name" innerRadius="55%" outerRadius="80%" paddingAngle={3}>
                      {genderData.map((entry) => (
                        <Cell key={entry.name} fill={GENDER_COLORS[entry.name] || '#94A3B8'} stroke="none" />
                      ))}
                    </Pie>
                    <Tooltip contentStyle={tooltipStyle} />
                    <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                  </PieChart>
                ) : <EmptyChart />}
              </ChartCard>
            </Grid>
          </>
        )}
      </Grid>
    </Layout>
  );
}
