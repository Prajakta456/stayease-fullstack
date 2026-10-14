import { createContext, useContext, useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { logoutApi, SESSION_EXPIRED_EVENT } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore user from localStorage on page reload
    const stored = localStorage.getItem('airbnb_user');
    if (stored) {
      try {
        setUser(JSON.parse(stored));
      } catch {
        localStorage.removeItem('airbnb_user');
      }
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    // Fired by the API client when the backend rejects our token (expired or revoked)
    const onSessionExpired = () => {
      setUser(null);
      toast.error('Your session has expired. Please sign in again.');
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
  }, []);

  const login = (userData) => {
    setUser(userData);
    localStorage.setItem('airbnb_user', JSON.stringify(userData));
  };

  const logout = async () => {
    try {
      await logoutApi(); // revoke the token server-side
    } catch {
      // Already expired/invalid — signing out locally is enough
    }
    setUser(null);
    localStorage.removeItem('airbnb_user');
  };

  const isHotelManager = user?.roles?.includes('HOTEL_MANAGER');
  const isGuest = user?.roles?.includes('GUEST');

  return (
    <AuthContext.Provider value={{ user, login, logout, isHotelManager, isGuest, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
