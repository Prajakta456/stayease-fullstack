import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const POPULAR_CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Goa', 'Jaipur', 'Hyderabad'];

export default function HomePage() {
  const navigate = useNavigate();
  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];

  const [search, setSearch] = useState({
    city: '',
    startDate: today,
    endDate: tomorrow,
    roomsCount: 1,
  });

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams({
      city: search.city,
      startDate: search.startDate,
      endDate: search.endDate,
      roomsCount: search.roomsCount,
    });
    navigate(`/search?${params}`);
  };

  return (
    <>
      {/* HERO */}
      <section className="hero">
        <div className="hero-bg" />
        <div className="hero-grid" />
        <div className="hero-content">
          <div className="hero-eyebrow animate-in">
            <span>✦</span> Over 10,000 stays across India
          </div>
          <h1 className="hero-title animate-in stagger-1">
            Find where
            <span className="accent">you belong.</span>
          </h1>
          <p className="hero-subtitle animate-in stagger-2">
            Curated hotels, transparent pricing, seamless booking — from mountain retreats to city escapes.
          </p>

          {/* Search box */}
          <form className="search-box animate-in stagger-3" onSubmit={handleSearch}>
            <div className="search-field">
              <div className="search-field-label">Destination</div>
              <input
                type="text"
                placeholder="City or hotel name"
                value={search.city}
                onChange={e => setSearch(p => ({ ...p, city: e.target.value }))}
                list="city-list"
              />
              <datalist id="city-list">
                {POPULAR_CITIES.map(c => <option key={c} value={c} />)}
              </datalist>
            </div>
            <div className="search-divider" />
            <div className="search-field">
              <div className="search-field-label">Check-in</div>
              <input
                type="date"
                value={search.startDate}
                min={today}
                onChange={e => setSearch(p => ({ ...p, startDate: e.target.value }))}
              />
            </div>
            <div className="search-divider" />
            <div className="search-field">
              <div className="search-field-label">Check-out</div>
              <input
                type="date"
                value={search.endDate}
                min={search.startDate}
                onChange={e => setSearch(p => ({ ...p, endDate: e.target.value }))}
              />
            </div>
            <div className="search-divider" />
            <div className="search-field" style={{ minWidth: 80 }}>
              <div className="search-field-label">Rooms</div>
              <select
                value={search.roomsCount}
                onChange={e => setSearch(p => ({ ...p, roomsCount: +e.target.value }))}
              >
                {[1,2,3,4,5].map(n => <option key={n} value={n}>{n} Room{n > 1 ? 's' : ''}</option>)}
              </select>
            </div>
            <button type="submit" className="search-submit">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
              </svg>
              Search
            </button>
          </form>

          {/* Popular cities */}
          <div className="animate-in stagger-4" style={{ marginTop: 24, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <span style={{ color: 'rgba(255,255,255,0.4)', fontSize: '0.82rem', alignSelf: 'center' }}>Popular:</span>
            {POPULAR_CITIES.map(city => (
              <button
                key={city}
                onClick={() => navigate(`/search?city=${city}&startDate=${today}&endDate=${tomorrow}&roomsCount=1`)}
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  border: '1px solid rgba(255,255,255,0.2)',
                  color: 'rgba(255,255,255,0.8)',
                  padding: '6px 14px',
                  borderRadius: '100px',
                  fontSize: '0.82rem',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
                onMouseOver={e => e.target.style.background = 'rgba(200,169,126,0.25)'}
                onMouseOut={e => e.target.style.background = 'rgba(255,255,255,0.1)'}
              >
                {city}
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* FEATURES STRIP */}
      <section style={{ background: 'var(--white)', padding: '60px 24px' }}>
        <div className="container">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 40 }}>
            {[
              { icon: '🔍', title: 'Smart Search', desc: 'Dynamic pricing that updates in real-time based on demand' },
              { icon: '🏨', title: 'Curated Hotels', desc: 'Every property vetted by our team for quality and comfort' },
              { icon: '📋', title: 'Instant Booking', desc: 'Reserve your room in seconds, no lengthy processes' },
              { icon: '💰', title: 'Best Price', desc: 'Transparent pricing — what you see is what you pay' },
            ].map((f, i) => (
              <div key={i} className="animate-in" style={{ animationDelay: `${i * 0.08}s` }}>
                <div style={{ fontSize: '2.2rem', marginBottom: 12 }}>{f.icon}</div>
                <h3 style={{ fontFamily: 'var(--font-display)', fontSize: '1.1rem', fontWeight: 700, marginBottom: 6 }}>{f.title}</h3>
                <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem', lineHeight: 1.6 }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
