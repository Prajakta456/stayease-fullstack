import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function ProtectedRoute({ children, requiredRole }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Loading…</div>;

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (requiredRole && !user.roles?.includes(requiredRole)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
