import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

export default function Navbar() {
  const loc = useLocation();
  const navigate = useNavigate();
  const { user, logout, isHotelManager } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  // The account menu opens on click (works on touch screens and with the keyboard), and closes on
  // an outside click, Escape, or navigation
  useEffect(() => {
    if (!menuOpen) return undefined;
    const onClick = (e) => { if (!menuRef.current?.contains(e.target)) setMenuOpen(false); };
    const onKey = (e) => { if (e.key === 'Escape') setMenuOpen(false); };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [menuOpen]);

  useEffect(() => setMenuOpen(false), [loc.pathname]);

  const handleLogout = async () => {
    setMenuOpen(false);
    await logout();
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
            <div className={`navbar-user ${menuOpen ? 'open' : ''}`} ref={menuRef}>
              <button
                type="button"
                className="navbar-avatar"
                title={user.name}
                aria-label={`Account menu for ${user.name}`}
                aria-haspopup="true"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen(open => !open)}
              >
                {user.name?.charAt(0)?.toUpperCase()}
              </button>
              <div className="navbar-dropdown" role="menu">
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
