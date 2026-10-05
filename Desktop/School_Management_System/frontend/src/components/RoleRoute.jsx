import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { canAccessRole } from '../utils/permissions';

/**
 * Route guard enforcing role-based access.
 * Usage: <RoleRoute roles={[ROLES.TEACHER]}><Page /></RoleRoute>
 * Omitting `roles` allows any authenticated user.
 */
export default function RoleRoute({ children, roles }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return children;
}
