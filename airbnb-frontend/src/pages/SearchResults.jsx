import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { searchHotels } from '../services/api';

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
            ₹{price.toLocaleString('en-IN')}
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
                <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '1.3rem' }}>
                  ₹{price.toLocaleString('en-IN')}
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
  const [currentPage, setCurrentPage] = useState(0);

  const city = params.get('city') || '';
  const startDate = params.get('startDate') || '';
  const endDate = params.get('endDate') || '';
  const roomsCount = parseInt(params.get('roomsCount') || '1');

  const [filters, setFilters] = useState({ city, startDate, endDate, roomsCount });

  useEffect(() => {
    doSearch(0);
    // eslint-disable-next-line
  }, [params]);

  const doSearch = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const data = await searchHotels({
        city: params.get('city') || '',
        startDate: params.get('startDate'),
        endDate: params.get('endDate'),
        roomsCount: parseInt(params.get('roomsCount') || '1'),
        page,
        size: 9,
      });
      setResults(data.content || []);
      setTotalPages(data.totalPages || 0);
      setCurrentPage(page);
    } catch (e) {
      setError('Could not load results. Make sure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    setParams({
      city: filters.city,
      startDate: filters.startDate,
      endDate: filters.endDate,
      roomsCount: filters.roomsCount,
    });
  };

  const nights = startDate && endDate
    ? Math.max(1, Math.round((new Date(endDate) - new Date(startDate)) / 86400000))
    : 1;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--ash)' }}>
      {/* Filter bar */}
      <div style={{ background: 'var(--white)', borderBottom: '1px solid var(--ash-dark)', padding: '16px 24px' }}>
        <div className="container">
          <form className="flex items-center gap-4" onSubmit={handleFilterSubmit} style={{ flexWrap: 'wrap' }}>
            <div className="form-group" style={{ flex: '1 1 160px' }}>
              <label className="form-label">City</label>
              <input
                className="form-input"
                placeholder="Any city"
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
                onChange={e => setFilters(p => ({ ...p, startDate: e.target.value }))}
              />
            </div>
            <div className="form-group" style={{ flex: '1 1 140px' }}>
              <label className="form-label">Check-out</label>
              <input
                className="form-input"
                type="date"
                value={filters.endDate}
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
            {city ? `Hotels in ${city}` : 'All Hotels'}
          </h1>
          {startDate && endDate && (
            <p className="page-subtitle">
              {new Date(startDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
              {' → '}
              {new Date(endDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
              {' · '}{nights} night{nights > 1 ? 's' : ''}
              {' · '}{roomsCount} room{roomsCount > 1 ? 's' : ''}
            </p>
          )}
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

        {!loading && results.length > 0 && (
          <>
            <p style={{ color: 'var(--ink-mid)', marginBottom: 20, fontSize: '0.88rem' }}>
              {results.length} propert{results.length === 1 ? 'y' : 'ies'} found
            </p>
            <div className="hotel-grid">
              {results.map((item, i) => (
                <HotelCard
                  key={item.hotel?.id ?? i}
                  hotel={item.hotel || item}
                  price={item.price}
                  onClick={() => navigate(`/hotels/${item.hotel?.id || item.id}`, {
                    state: { startDate, endDate, roomsCount, nights }
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
                  onClick={() => doSearch(currentPage - 1)}
                >
                  ← Previous
                </button>
                <span style={{ color: 'var(--ink-mid)', fontSize: '0.9rem' }}>
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  className="btn btn-outline"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => doSearch(currentPage + 1)}
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
