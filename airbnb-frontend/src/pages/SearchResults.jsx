import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { searchHotels, getErrorMessage } from '../services/api';
import { todayISO, addDaysISO, nightsBetween, formatDate, formatINR } from '../utils/format';

const PAGE_SIZE = 9;

function HotelCard({ hotel, price, onClick }) {
  const amenities = hotel.amenities || [];

  return (
    <div className="hotel-card animate-in" onClick={onClick}>
      <div className="hotel-card-img">
        {hotel.photos && hotel.photos[0] ? (
          <img src={hotel.photos[0]} alt={hotel.name} />
        ) : (
          <div className="hotel-card-img-placeholder">{hotel.name}</div>
        )}
        {price != null && (
          <div className="hotel-card-price-badge">
            {formatINR(price)}
          </div>
        )}
      </div>
      <div className="hotel-card-body">
        <div className="hotel-card-city">{hotel.city}</div>
        <div className="hotel-card-name">{hotel.name}</div>
        <div className="hotel-card-amenities">
          {amenities.slice(0, 4).map((a, i) => (
            <span key={i} className="amenity-tag">{a}</span>
          ))}
          {amenities.length > 4 && (
            <span className="amenity-tag">+{amenities.length - 4} more</span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            {price != null && (
              <div>
                <span style={{ color: 'var(--ink-mid)', fontSize: '0.8rem' }}>from </span>
                <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '1.3rem' }}>
                  {formatINR(price)}
                </span>
                <span style={{ color: 'var(--ink-mid)', fontSize: '0.8rem' }}> / night</span>
              </div>
            )}
          </div>
          <button className="btn btn-clay btn-sm">Book Now</button>
        </div>
      </div>
    </div>
  );
}

export default function SearchResults() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();

  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // The URL is the single source of truth, so refresh, back/forward and shared links all work
  const city = params.get('city') || '';
  const startDate = params.get('startDate') || todayISO();
  const endDate = params.get('endDate') || addDaysISO(startDate, 1);
  const roomsCount = parseInt(params.get('roomsCount') || '1', 10);
  const currentPage = Math.max(0, parseInt(params.get('page') || '0', 10) || 0);
  const nights = nightsBetween(startDate, endDate);

  const [filters, setFilters] = useState({ city, startDate, endDate, roomsCount });

  // Keep the filter inputs in sync when the URL changes (back/forward, popular-city links)
  useEffect(() => {
    setFilters({ city, startDate, endDate, roomsCount });
  }, [city, startDate, endDate, roomsCount]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    searchHotels({ city, startDate, endDate, roomsCount, page: currentPage, size: PAGE_SIZE })
      .then(data => {
        if (cancelled) return;
        setResults(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
      })
      .catch(e => {
        if (!cancelled) setError(getErrorMessage(e, 'Could not load results. Please try again.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [city, startDate, endDate, roomsCount, currentPage]);

  const updateParams = (next) => {
    setParams({
      city: next.city,
      startDate: next.startDate,
      endDate: next.endDate,
      roomsCount: next.roomsCount,
      ...(next.page ? { page: next.page } : {}),
    });
  };

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    updateParams({ ...filters, page: 0 });
  };

  const goToPage = (page) => {
    updateParams({ city, startDate, endDate, roomsCount, page });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleFilterCheckIn = (value) => setFilters(p => ({
    ...p,
    startDate: value,
    endDate: value && nightsBetween(value, p.endDate) === 0 ? addDaysISO(value, 1) : p.endDate,
  }));

  return (
    <div style={{ minHeight: '100vh', background: 'var(--ash)' }}>
      {/* Filter bar */}
      <div style={{ background: 'var(--white)', borderBottom: '1px solid var(--ash-dark)', padding: '16px 24px' }}>
        <div className="container">
          <form className="flex items-center gap-4" onSubmit={handleFilterSubmit} style={{ flexWrap: 'wrap' }}>
            <div className="form-group" style={{ flex: '1 1 160px' }}>
              <label className="form-label">City or hotel</label>
              <input
                className="form-input"
                placeholder="Anywhere"
                value={filters.city}
                onChange={e => setFilters(p => ({ ...p, city: e.target.value }))}
              />
            </div>
            <div className="form-group" style={{ flex: '1 1 140px' }}>
              <label className="form-label">Check-in</label>
              <input
                className="form-input"
                type="date"
                value={filters.startDate}
                min={todayISO()}
                required
                onChange={e => handleFilterCheckIn(e.target.value)}
              />
            </div>
            <div className="form-group" style={{ flex: '1 1 140px' }}>
              <label className="form-label">Check-out</label>
              <input
                className="form-input"
                type="date"
                value={filters.endDate}
                min={addDaysISO(filters.startDate || todayISO(), 1)}
                required
                onChange={e => setFilters(p => ({ ...p, endDate: e.target.value }))}
              />
            </div>
            <div className="form-group" style={{ flex: '0 1 100px' }}>
              <label className="form-label">Rooms</label>
              <select
                className="form-input"
                value={filters.roomsCount}
                onChange={e => setFilters(p => ({ ...p, roomsCount: +e.target.value }))}
              >
                {[1,2,3,4,5].map(n => <option key={n} value={n}>{n}</option>)}
              </select>
            </div>
            <button type="submit" className="btn btn-clay" style={{ alignSelf: 'flex-end' }}>
              Update
            </button>
          </form>
        </div>
      </div>

      <div className="container" style={{ padding: '40px 24px' }}>
        {/* Header */}
        <div style={{ marginBottom: 32 }}>
          <h1 className="page-title">
            {city ? `Stays matching “${city}”` : 'All Hotels'}
          </h1>
          <p className="page-subtitle">
            {formatDate(startDate, { day: 'numeric', month: 'short' })}
            {' → '}
            {formatDate(endDate, { day: 'numeric', month: 'short' })}
            {' · '}{nights} night{nights === 1 ? '' : 's'}
            {' · '}{roomsCount} room{roomsCount > 1 ? 's' : ''}
          </p>
        </div>

        {loading && (
          <div className="loading-center">
            <div className="spinner" />
          </div>
        )}

        {error && (
          <div style={{
            background: '#fde8e8', border: '1px solid #f5c6c6',
            borderRadius: 'var(--radius)', padding: 24, color: 'var(--red)'
          }}>
            {error}
          </div>
        )}

        {!loading && !error && results.length === 0 && (
          <div className="empty-state">
            <div className="empty-state-icon">🏨</div>
            <h3>No hotels found</h3>
            <p>Try adjusting your search — different city or dates.</p>
          </div>
        )}

        {!loading && !error && results.length > 0 && (
          <>
            <p style={{ color: 'var(--ink-mid)', marginBottom: 20, fontSize: '0.88rem' }}>
              {totalElements} propert{totalElements === 1 ? 'y' : 'ies'} found
            </p>
            <div className="hotel-grid">
              {results.map(item => (
                <HotelCard
                  key={item.hotel.id}
                  hotel={item.hotel}
                  price={item.price}
                  onClick={() => navigate(`/hotels/${item.hotel.id}`, {
                    state: { startDate, endDate, roomsCount }
                  })}
                />
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between" style={{ marginTop: 40 }}>
                <button
                  className="btn btn-outline"
                  disabled={currentPage === 0}
                  onClick={() => goToPage(currentPage - 1)}
                >
                  ← Previous
                </button>
                <span style={{ color: 'var(--ink-mid)', fontSize: '0.9rem' }}>
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  className="btn btn-outline"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => goToPage(currentPage + 1)}
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
