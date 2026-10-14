import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { signup, getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

export default function SignupPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'GUEST',
  });
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    // Same rule as the backend (bcrypt only uses the first 72 bytes)
    if (form.password.length < 8 || form.password.length > 72) {
      toast.error('Password must be between 8 and 72 characters');
      return;
    }
    setLoading(true);
    try {
      const { confirmPassword, ...payload } = form;
      const userData = await signup(payload);
      login(userData);
      toast.success(`Welcome, ${userData.name}! Account created successfully.`);
      const isManager = userData.roles?.includes('HOTEL_MANAGER');
      // Guests return to the hotel they were booking, if they came from one
      const from = location.state?.from;
      navigate(isManager ? '/admin' : from || '/', { replace: true, state: location.state?.fromState });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not create your account. Please try again.'));
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
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">
          Already have an account?{' '}
          <Link to="/login" state={location.state} className="auth-link">Sign in</Link>
        </p>

        {/* Role toggle */}
        <div className="role-toggle">
          <button
            type="button"
            className={`role-btn ${form.role === 'GUEST' ? 'active' : ''}`}
            onClick={() => setForm(f => ({ ...f, role: 'GUEST' }))}
          >
            🏖️ Guest
          </button>
          <button
            type="button"
            className={`role-btn ${form.role === 'HOTEL_MANAGER' ? 'active' : ''}`}
            onClick={() => setForm(f => ({ ...f, role: 'HOTEL_MANAGER' }))}
          >
            🏨 Hotel Manager
          </button>
        </div>
        <p style={{ fontSize: '0.78rem', color: 'var(--ink-mid)', textAlign: 'center', marginBottom: 20 }}>
          {form.role === 'HOTEL_MANAGER'
            ? 'You can list and manage your hotels & rooms'
            : 'You can browse and book hotels'}
        </p>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input
              className="form-input"
              type="text"
              name="name"
              placeholder="John Doe"
              value={form.name}
              onChange={handleChange}
              required
              autoComplete="name"
            />
          </div>

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
              placeholder="At least 8 characters"
              minLength={8}
              maxLength={72}
              value={form.password}
              onChange={handleChange}
              required
              autoComplete="new-password"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input
              className="form-input"
              type="password"
              name="confirmPassword"
              placeholder="Repeat your password"
              value={form.confirmPassword}
              onChange={handleChange}
              required
              autoComplete="new-password"
            />
          </div>

          <button
            type="submit"
            className="btn btn-clay"
            style={{ width: '100%', justifyContent: 'center', padding: '14px', fontSize: '1rem', marginTop: 8 }}
            disabled={loading}
          >
            {loading ? 'Creating account…' : `Sign up as ${form.role === 'HOTEL_MANAGER' ? 'Hotel Manager' : 'Guest'}`}
          </button>
        </form>
      </div>
    </div>
  );
}
