import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { loginApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const from = location.state?.from || '/';

  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const userData = await loginApi(form);
      login(userData);
      toast.success(`Welcome back, ${userData.name}!`);
      // Redirect based on role
      const isManager = userData.roles?.includes('HOTEL_MANAGER');
      navigate(isManager ? '/admin' : from, { replace: true });
    } catch (err) {
      const msg = err.response?.data?.error?.message || err.response?.data?.message || 'Invalid email or password';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          Stay<span>Ease</span>
        </div>
        <h1 className="auth-title">Sign in to your account</h1>
        <p className="auth-subtitle">
          Don't have an account?{' '}
          <Link to="/signup" className="auth-link">Create one free</Link>
        </p>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Email address</label>
            <input
              className="form-input"
              type="email"
              name="email"
              placeholder="you@example.com"
              value={form.email}
              onChange={handleChange}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              className="form-input"
              type="password"
              name="password"
              placeholder="••••••••"
              value={form.password}
              onChange={handleChange}
              required
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className="btn btn-clay"
            style={{ width: '100%', justifyContent: 'center', padding: '14px', fontSize: '1rem', marginTop: 8 }}
            disabled={loading}
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="auth-divider">
          <span>Demo credentials</span>
        </div>
        <div className="auth-demo-creds">
          <div>
            <strong>Guest:</strong> guest@demo.com / password123
          </div>
          <div>
            <strong>Manager:</strong> manager@demo.com / password123
          </div>
        </div>
      </div>
    </div>
  );
}
