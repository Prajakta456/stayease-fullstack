import { useNavigate } from 'react-router-dom';

export default function MyBookings() {
  const navigate = useNavigate();
  return (
    <div style={{ minHeight: '80vh', background: 'var(--ash)', padding: '60px 24px' }}>
      <div className="container" style={{ maxWidth: 700 }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2rem', fontWeight: 800, marginBottom: 8 }}>
          My Bookings
        </h1>
        <p style={{ color: 'var(--ink-mid)', marginBottom: 40 }}>
          Your upcoming and past stays.
        </p>
        {/* ✅ Bookings list — backend endpoint GET /bookings not yet implemented */}
        <div className="empty-state">
          <div className="empty-state-icon">🧳</div>
          <h3>No bookings yet</h3>
          <p>Your confirmed bookings will appear here.</p>
          <button className="btn btn-clay" onClick={() => navigate('/')} style={{ marginTop: 20 }}>
            Explore Hotels
          </button>
        </div>
      </div>
    </div>
  );
}
