import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createHotel, getAllHotels, getHotelById, updateHotel, deleteHotel, activateHotel } from '../services/api';
import toast from 'react-hot-toast';

const EMPTY_HOTEL = {
  name: '', city: '', photos: [], amenities: [], active: false,
  contactInfo: { phoneNumber: '', email: '', address: '', website: '' }
};

function HotelFormModal({ hotel, onClose, onSaved }) {
  const [form, setForm] = useState(hotel || EMPTY_HOTEL);
  const [saving, setSaving] = useState(false);
  const isEdit = !!hotel?.id;

  const handleChange = (field, val) => setForm(f => ({ ...f, [field]: val }));
  const handleContact = (field, val) => setForm(f => ({
    ...f, contactInfo: { ...f.contactInfo, [field]: val }
  }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        photos: typeof form.photos === 'string'
          ? form.photos.split('\n').map(s => s.trim()).filter(Boolean)
          : form.photos,
        amenities: typeof form.amenities === 'string'
          ? form.amenities.split(',').map(s => s.trim()).filter(Boolean)
          : form.amenities,
      };
      let saved;
      if (isEdit) {
        saved = await updateHotel(hotel.id, payload);
        toast.success('Hotel updated!');
      } else {
        saved = await createHotel(payload);
        toast.success('Hotel created!');
      }
      onSaved(saved);
      onClose();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to save hotel');
    } finally {
      setSaving(false);
    }
  };

  const photosStr = Array.isArray(form.photos) ? form.photos.join('\n') : form.photos;
  const amenitiesStr = Array.isArray(form.amenities) ? form.amenities.join(', ') : form.amenities;

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ maxWidth: 620 }}>
        <div className="modal-header">
          <h2 className="modal-title">{isEdit ? 'Edit Hotel' : 'Create New Hotel'}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div className="form-group">
                  <label className="form-label">Hotel Name *</label>
                  <input className="form-input" required value={form.name} onChange={e => handleChange('name', e.target.value)} placeholder="Grand Palace" />
                </div>
                <div className="form-group">
                  <label className="form-label">City *</label>
                  <input className="form-input" required value={form.city} onChange={e => handleChange('city', e.target.value)} placeholder="Mumbai" />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Amenities (comma-separated)</label>
                <input className="form-input" value={amenitiesStr} onChange={e => handleChange('amenities', e.target.value)} placeholder="WiFi, Pool, Gym, Spa" />
              </div>
              <div className="form-group">
                <label className="form-label">Photo URLs (one per line)</label>
                <textarea className="form-input" rows={3} value={photosStr} onChange={e => handleChange('photos', e.target.value)} placeholder="https://example.com/photo.jpg" style={{ resize: 'vertical' }} />
              </div>
              <div style={{ borderTop: '1px solid var(--ash-dark)', paddingTop: 16 }}>
                <p style={{ fontSize: '0.82rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--ink-mid)', marginBottom: 12 }}>Contact Information</p>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div className="form-group">
                    <label className="form-label">Phone</label>
                    <input className="form-input" value={form.contactInfo?.phoneNumber || ''} onChange={e => handleContact('phoneNumber', e.target.value)} placeholder="+91 98765 43210" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Email</label>
                    <input className="form-input" type="email" value={form.contactInfo?.email || ''} onChange={e => handleContact('email', e.target.value)} placeholder="hello@hotel.com" />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label">Address</label>
                    <input className="form-input" value={form.contactInfo?.address || ''} onChange={e => handleContact('address', e.target.value)} placeholder="123 Main St, Mumbai 400001" />
                  </div>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <input
                  type="checkbox"
                  id="active"
                  checked={form.active || false}
                  onChange={e => handleChange('active', e.target.checked)}
                  style={{ width: 18, height: 18, accentColor: 'var(--clay)' }}
                />
                <label htmlFor="active" style={{ fontSize: '0.9rem', fontWeight: 500, cursor: 'pointer' }}>
                  Mark as Active (visible to guests)
                </label>
              </div>
              <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', paddingTop: 8 }}>
                <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
                <button type="submit" className="btn btn-clay" disabled={saving}>
                  {saving ? 'Saving...' : isEdit ? 'Update Hotel' : 'Create Hotel'}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('hotels');
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true); // ✅ NEW: loading state
  const [showModal, setShowModal] = useState(false);
  const [editingHotel, setEditingHotel] = useState(null);
  const [loadingId, setLoadingId] = useState(null);

  const [searchId, setSearchId] = useState('');
  const [fetchedHotel, setFetchedHotel] = useState(null);
  const [fetching, setFetching] = useState(false);

  // ✅ FIXED: fetch all hotels from DB on mount
  useEffect(() => {
    getAllHotels()
      .then(data => setHotels(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Could not load hotels'))
      .finally(() => setLoading(false));
  }, []);

  const openCreate = () => { setEditingHotel(null); setShowModal(true); };
  const openEdit = (hotel) => { setEditingHotel(hotel); setShowModal(true); };

  const handleSaved = (hotel) => {
    setHotels(prev => {
      const idx = prev.findIndex(h => h.id === hotel.id);
      if (idx >= 0) return prev.map(h => h.id === hotel.id ? hotel : h);
      return [...prev, hotel];
    });
  };

  const handleDelete = async (hotelId) => {
    if (!window.confirm('Delete this hotel? This cannot be undone.')) return;
    setLoadingId(hotelId);
    try {
      await deleteHotel(hotelId);
      setHotels(prev => prev.filter(h => h.id !== hotelId));
      toast.success('Hotel deleted');
    } catch {
      toast.error('Could not delete hotel');
    } finally {
      setLoadingId(null);
    }
  };

  const handleActivate = async (hotelId) => {
    setLoadingId(hotelId);
    try {
      await activateHotel(hotelId);
      setHotels(prev => prev.map(h => h.id === hotelId ? { ...h, active: true } : h));
      toast.success('Hotel activated');
    } catch {
      toast.error('Could not activate hotel');
    } finally {
      setLoadingId(null);
    }
  };

  const handleFetchById = async () => {
    if (!searchId) return;
    setFetching(true);
    try {
      const h = await getHotelById(searchId);
      setFetchedHotel(h);
      if (!hotels.find(hotel => hotel.id === h.id)) {
        setHotels(prev => [...prev, h]);
      }
    } catch {
      toast.error('Hotel not found');
      setFetchedHotel(null);
    } finally {
      setFetching(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--ash)' }}>
      {/* Admin header */}
      <div style={{ background: 'var(--ink)', padding: '40px 24px 32px' }}>
        <div className="container">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
            <div>
              <div style={{ color: 'var(--clay)', fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 6 }}>
                Admin Panel
              </div>
              <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2.4rem', fontWeight: 800, color: 'var(--white)', letterSpacing: '-1px' }}>
                Hotel Management
              </h1>
            </div>
            <button className="btn btn-clay" onClick={openCreate}>
              + New Hotel
            </button>
          </div>

          <div className="tabs" style={{ marginTop: 28, width: 'fit-content', background: 'rgba(255,255,255,0.1)' }}>
            {['hotels', 'rooms', 'lookup'].map(t => (
              <button
                key={t}
                className={`tab ${activeTab === t ? 'active' : ''}`}
                onClick={() => setActiveTab(t)}
                style={{ color: activeTab === t ? 'var(--ink)' : 'rgba(255,255,255,0.7)' }}
              >
                {t.charAt(0).toUpperCase() + t.slice(1)}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="container" style={{ padding: '32px 24px' }}>

        {/* Hotels Tab */}
        {activeTab === 'hotels' && (
          <div>
            {/* ✅ NEW: show spinner while loading */}
            {loading ? (
              <div className="loading-center"><div className="spinner" /></div>
            ) : hotels.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon">🏨</div>
                <h3>No hotels yet</h3>
                <p>Create your first hotel to get started.</p>
                <button className="btn btn-clay" onClick={openCreate} style={{ marginTop: 20 }}>Create Hotel</button>
              </div>
            ) : (
              <div className="card">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Name</th>
                      <th>City</th>
                      <th>Status</th>
                      <th>Amenities</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {hotels.map(hotel => (
                      <tr key={hotel.id}>
                        <td style={{ color: 'var(--ink-mid)', fontWeight: 500 }}>#{hotel.id}</td>
                        <td style={{ fontWeight: 600 }}>{hotel.name}</td>
                        <td>{hotel.city}</td>
                        <td>
                          <span className={`badge ${hotel.active ? 'badge-green' : 'badge-gray'}`}>
                            {hotel.active ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td>
                          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                            {(hotel.amenities || []).slice(0, 3).map((a, i) => (
                              <span key={i} className="amenity-tag">{a}</span>
                            ))}
                            {(hotel.amenities || []).length > 3 && (
                              <span className="amenity-tag">+{hotel.amenities.length - 3}</span>
                            )}
                          </div>
                        </td>
                        <td>
                          <div style={{ display: 'flex', gap: 6 }}>
                            <button className="btn btn-ghost btn-sm" onClick={() => openEdit(hotel)}>Edit</button>
                            {!hotel.active && (
                              <button
                                className="btn btn-sm"
                                style={{ background: 'var(--green)', color: 'white' }}
                                disabled={loadingId === hotel.id}
                                onClick={() => handleActivate(hotel.id)}
                              >
                                Activate
                              </button>
                            )}
                            <button
                              className="btn btn-sm"
                              style={{ background: 'var(--ash)', color: 'var(--ink-mid)' }}
                              onClick={() => navigate(`/admin/hotels/${hotel.id}/rooms`)}
                            >
                              Rooms
                            </button>
                            <button
                              className="btn btn-danger btn-sm"
                              disabled={loadingId === hotel.id}
                              onClick={() => handleDelete(hotel.id)}
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Rooms Tab */}
        {activeTab === 'rooms' && (
          <div>
            <div className="card">
              <div className="card-body">
                <h2 className="section-title" style={{ marginBottom: 12 }}>Manage Rooms</h2>
                <p style={{ color: 'var(--ink-mid)', marginBottom: 20, fontSize: '0.9rem' }}>
                  Select a hotel to manage its rooms.
                </p>
                {loading ? (
                  <div className="loading-center"><div className="spinner" /></div>
                ) : hotels.length === 0 ? (
                  <p style={{ color: 'var(--ink-mid)' }}>No hotels found. Create a hotel first.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {hotels.map(hotel => (
                      <div key={hotel.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', background: 'var(--ash)', borderRadius: 'var(--radius-sm)' }}>
                        <div>
                          <span style={{ fontWeight: 600 }}>{hotel.name}</span>
                          <span style={{ color: 'var(--ink-mid)', fontSize: '0.85rem', marginLeft: 8 }}>{hotel.city}</span>
                        </div>
                        <button
                          className="btn btn-clay btn-sm"
                          onClick={() => navigate(`/admin/hotels/${hotel.id}/rooms`)}
                        >
                          Manage Rooms →
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Lookup Tab */}
        {activeTab === 'lookup' && (
          <div>
            <div className="card" style={{ maxWidth: 480 }}>
              <div className="card-body">
                <h2 className="section-title" style={{ marginBottom: 8 }}>Lookup Hotel by ID</h2>
                <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem', marginBottom: 20 }}>
                  Enter a hotel ID to fetch and edit it.
                </p>
                <div style={{ display: 'flex', gap: 10 }}>
                  <input
                    className="form-input"
                    type="number"
                    placeholder="Hotel ID"
                    value={searchId}
                    onChange={e => setSearchId(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleFetchById()}
                  />
                  <button className="btn btn-clay" onClick={handleFetchById} disabled={fetching}>
                    {fetching ? '...' : 'Fetch'}
                  </button>
                </div>

                {fetchedHotel && (
                  <div style={{ marginTop: 20, padding: 16, background: 'var(--ash)', borderRadius: 'var(--radius-sm)' }}>
                    <p style={{ fontWeight: 700, marginBottom: 4 }}>{fetchedHotel.name}</p>
                    <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem', marginBottom: 12 }}>{fetchedHotel.city}</p>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-clay btn-sm" onClick={() => openEdit(fetchedHotel)}>Edit</button>
                      <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/admin/hotels/${fetchedHotel.id}/rooms`)}>Rooms</button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {showModal && (
        <HotelFormModal
          hotel={editingHotel}
          onClose={() => { setShowModal(false); setEditingHotel(null); }}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}
