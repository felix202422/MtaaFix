import { Skeleton, Box, Card } from '@mui/material';

export function TableSkeleton({ rows = 5 }) {
  return (
    <Box sx={{ p: 2 }}>
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} variant="rectangular" height={48} sx={{ mb: 1, borderRadius: 1 }} />
      ))}
    </Box>
  );
}

export function StatsCardSkeleton() {
  return (
    <Card sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
      <Skeleton variant="circular" width={48} height={48} />
      <Box sx={{ flexGrow: 1 }}>
        <Skeleton width="60%" />
        <Skeleton width="40%" height={32} />
      </Box>
    </Card>
  );
}
