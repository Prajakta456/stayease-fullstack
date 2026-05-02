import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getHotelById, getAllRooms, createRoom, deleteRoom } from '../services/api';
import toast from 'react-hot-toast';

const EMPTY_ROOM = {
  type: '', basePrice: '', photos: [], amenities: [], totalCount: 1, capacity: 2
};

function RoomModal({ hotelId, onClose, onSaved }) {
  const [form, setForm] = useState(EMPTY_ROOM);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        basePrice: parseFloat(form.basePrice),
        totalCount: parseInt(form.totalCount),
        capacity: parseInt(form.capacity),
        amenities: typeof form.amenities === 'string'
          ? form.amenities.split(',').map(s => s.trim()).filter(Boolean)
          : form.amenities,
        photos: typeof form.photos === 'string'
          ? form.photos.split('\n').map(s => s.trim()).filter(Boolean)
          : form.photos,
      };
      const saved = await createRoom(hotelId, payload);
      toast.success('Room created!');
      onSaved(saved);
      onClose();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to create room');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-header">
          <h2 className="modal-title">Add New Room</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="form-group">
                <label className="form-label">Room Type *</label>
                <input className="form-input" required value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))} placeholder="Deluxe Suite, Standard Room, etc." />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
                <div className="form-group">
                  <label className="form-label">Base Price (₹) *</label>
                  <input className="form-input" required type="number" min="0" value={form.basePrice} onChange={e => setForm(f => ({ ...f, basePrice: e.target.value }))} placeholder="2500" />
                </div>
                <div className="form-group">
                  <label className="form-label">Total Rooms *</label>
                  <input className="form-input" required type="number" min="1" value={form.totalCount} onChange={e => setForm(f => ({ ...f, totalCount: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Capacity *</label>
                  <input className="form-input" required type="number" min="1" value={form.capacity} onChange={e => setForm(f => ({ ...f, capacity: e.target.value }))} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Amenities (comma-separated)</label>
                <input className="form-input" value={form.amenities} onChange={e => setForm(f => ({ ...f, amenities: e.target.value }))} placeholder="AC, TV, WiFi, Mini-bar" />
              </div>
              <div className="form-group">
                <label className="form-label">Photo URLs (one per line)</label>
                <textarea className="form-input" rows={3} value={form.photos} onChange={e => setForm(f => ({ ...f, photos: e.target.value }))} placeholder="https://..." style={{ resize: 'vertical' }} />
              </div>
              <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
                <button type="submit" className="btn btn-clay" disabled={saving}>
                  {saving ? 'Creating...' : 'Create Room'}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default function RoomAdmin() {
  const { hotelId } = useParams();
  const navigate = useNavigate();
  const [hotel, setHotel] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    Promise.all([getHotelById(hotelId), getAllRooms(hotelId)])
      .then(([h, r]) => { setHotel(h); setRooms(r || []); })
      .catch(() => toast.error('Could not load rooms'))
      .finally(() => setLoading(false));
  }, [hotelId]);

  const handleDelete = async (roomId) => {
    if (!window.confirm('Delete this room?')) return;
    setDeletingId(roomId);
    try {
      await deleteRoom(hotelId, roomId);
      setRooms(prev => prev.filter(r => r.id !== roomId));
      toast.success('Room deleted');
    } catch {
      toast.error('Could not delete room');
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--ash)' }}>
      <div style={{ background: 'var(--ink)', padding: '40px 24px 32px' }}>
        <div className="container">
          <button
            className="btn btn-ghost btn-sm"
            style={{ background: 'rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.8)', marginBottom: 20 }}
            onClick={() => navigate('/admin')}
          >
            ← Admin
          </button>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
            <div>
              <div style={{ color: 'var(--clay)', fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 6 }}>
                {hotel?.city || 'Hotel'}
              </div>
              <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2.2rem', fontWeight: 800, color: 'var(--white)', letterSpacing: '-1px' }}>
                {hotel?.name || `Hotel #${hotelId}`} — Rooms
              </h1>
            </div>
            <button className="btn btn-clay" onClick={() => setShowModal(true)}>+ Add Room</button>
          </div>
        </div>
      </div>

      <div className="container" style={{ padding: '32px 24px' }}>
        {rooms.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🛏️</div>
            <h3>No rooms yet</h3>
            <p>Add the first room to this hotel.</p>
            <button className="btn btn-clay" onClick={() => setShowModal(true)} style={{ marginTop: 20 }}>Add Room</button>
          </div>
        ) : (
          <div className="card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Base Price</th>
                  <th>Total Rooms</th>
                  <th>Capacity</th>
                  <th>Amenities</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rooms.map(room => (
                  <tr key={room.id}>
                    <td style={{ color: 'var(--ink-mid)', fontWeight: 500 }}>#{room.id}</td>
                    <td style={{ fontWeight: 700 }}>{room.type}</td>
                    <td>
                      <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700 }}>
                        ₹{Number(room.basePrice).toLocaleString('en-IN')}
                      </span>
                      <span style={{ color: 'var(--ink-mid)', fontSize: '0.78rem' }}>/night</span>
                    </td>
                    <td>{room.totalCount}</td>
                    <td>{room.capacity} guests</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                        {(room.amenities || []).slice(0, 3).map((a, i) => (
                          <span key={i} className="amenity-tag">{a}</span>
                        ))}
                        {(room.amenities || []).length > 3 && (
                          <span className="amenity-tag">+{room.amenities.length - 3}</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <button
                        className="btn btn-danger btn-sm"
                        disabled={deletingId === room.id}
                        onClick={() => handleDelete(room.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showModal && (
        <RoomModal
          hotelId={hotelId}
          onClose={() => setShowModal(false)}
          onSaved={room => setRooms(prev => [...prev, room])}
        />
      )}
    </div>
  );
}
