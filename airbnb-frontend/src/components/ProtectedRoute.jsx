import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function ProtectedRoute({ children, requiredRole }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Loading…</div>;

  if (!user) {
    // Remember where the user was going (including query string and page state) for after sign-in
    return (
      <Navigate
        to="/login"
        state={{ from: location.pathname + location.search, fromState: location.state }}
        replace
      />
    );
  }

  if (requiredRole && !user.roles?.includes(requiredRole)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
