import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

export default function Navbar() {
  const loc = useLocation();
  const navigate = useNavigate();
  const { user, logout, isHotelManager } = useAuth();

  const handleLogout = () => {
    logout();
    toast.success('Signed out successfully');
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-logo">
          Stay<span>Ease</span>
        </Link>
        <div className="navbar-links">
          <Link to="/" className={`nav-link ${loc.pathname === '/' ? 'active' : ''}`}>
            Explore
          </Link>

          {/* Guest-only links */}
          {user && !isHotelManager && (
            <Link to="/bookings" className={`nav-link ${loc.pathname.startsWith('/bookings') ? 'active' : ''}`}>
              My Bookings
            </Link>
          )}

          {/* Manager-only links */}
          {user && isHotelManager && (
            <Link to="/admin" className={`nav-link ${loc.pathname.startsWith('/admin') ? 'active' : ''}`}>
              My Hotels
            </Link>
          )}

          {/* Auth buttons */}
          {!user ? (
            <>
              <Link to="/login" className={`nav-link ${loc.pathname === '/login' ? 'active' : ''}`}>
                Sign in
              </Link>
              <Link to="/signup" className="nav-btn">
                Get started
              </Link>
            </>
          ) : (
            <div className="navbar-user">
              <div className="navbar-avatar" title={user.name}>
                {user.name?.charAt(0)?.toUpperCase()}
              </div>
              <div className="navbar-dropdown">
                <div className="dropdown-name">{user.name}</div>
                <div className="dropdown-role">
                  {isHotelManager ? '🏨 Hotel Manager' : '🏖️ Guest'}
                </div>
                <hr style={{ margin: '8px 0', border: 'none', borderTop: '1px solid rgba(255,255,255,0.1)' }} />
                <button className="dropdown-btn" onClick={handleLogout}>
                  Sign out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
