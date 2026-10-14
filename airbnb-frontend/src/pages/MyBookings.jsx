import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { getMyBookings, cancelBooking, payForBooking, getErrorMessage } from '../services/api';
import { formatDate, formatINR, parseLocalDate } from '../utils/format';

const STATUS_BADGES = {
  RESERVED: { label: 'Awaiting guests', className: 'badge-yellow' },
  GUESTS_ADDED: { label: 'Awaiting payment', className: 'badge-yellow' },
  CONFIRMED: { label: 'Confirmed', className: 'badge-green' },
  CANCELLED: { label: 'Cancelled', className: 'badge-gray' },
  EXPIRED: { label: 'Expired', className: 'badge-red' },
};

const ACTIVE_STATUSES = ['RESERVED', 'GUESTS_ADDED', 'CONFIRMED'];

const isBeforeCheckIn = (booking) => parseLocalDate(booking.checkInDate) > new Date();

// Guests were added but payment never completed, and the rooms are still held
const canCompletePayment = (booking) =>
  booking.bookingStatus === 'GUESTS_ADDED' && booking.holdExpiresAt && new Date(booking.holdExpiresAt) > new Date();

export default function MyBookings() {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const [payingId, setPayingId] = useState(null);

  useEffect(() => {
    getMyBookings()
      .then(data => setBookings(Array.isArray(data) ? data : []))
      .catch(e => toast.error(getErrorMessage(e, 'Could not load your bookings')))
      .finally(() => setLoading(false));
  }, []);

  const handleCancel = async (booking) => {
    const prompt = booking.bookingStatus === 'CONFIRMED'
      ? 'Cancel this booking? You will be refunded in full.'
      : 'Cancel this reservation?';
    if (!window.confirm(prompt)) return;
    setCancellingId(booking.id);
    try {
      const updated = await cancelBooking(booking.id);
      setBookings(prev => prev.map(b => b.id === updated.id ? updated : b));
      toast.success('Booking cancelled');
    } catch (e) {
      toast.error(getErrorMessage(e, 'Could not cancel booking'));
    } finally {
      setCancellingId(null);
    }
  };

  const handlePay = async (booking) => {
    setPayingId(booking.id);
    try {
      const updated = await payForBooking(booking.id);
      setBookings(prev => prev.map(b => b.id === updated.id ? updated : b));
      toast.success('Payment complete — your booking is confirmed');
    } catch (e) {
      toast.error(getErrorMessage(e, 'Payment failed'));
    } finally {
      setPayingId(null);
    }
  };

  return (
    <div style={{ minHeight: '80vh', background: 'var(--ash)', padding: '60px 24px' }}>
      <div className="container" style={{ maxWidth: 760 }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2rem', fontWeight: 800, marginBottom: 8 }}>
          My Bookings
        </h1>
        <p style={{ color: 'var(--ink-mid)', marginBottom: 40 }}>
          Your upcoming and past stays.
        </p>

        {loading ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : bookings.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🧳</div>
            <h3>No bookings yet</h3>
            <p>Your bookings will appear here.</p>
            <button className="btn btn-clay" onClick={() => navigate('/')} style={{ marginTop: 20 }}>
              Explore Hotels
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {bookings.map(booking => {
              const badge = STATUS_BADGES[booking.bookingStatus] || { label: booking.bookingStatus, className: 'badge-gray' };
              const canCancel = ACTIVE_STATUSES.includes(booking.bookingStatus) && isBeforeCheckIn(booking);
              return (
                <div key={booking.id} className="card">
                  <div className="card-body" style={{ display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
                    <div>
                      <div style={{ color: 'var(--clay)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
                        {booking.hotelCity}
                      </div>
                      <h3 style={{ fontFamily: 'var(--font-display)', fontSize: '1.2rem', fontWeight: 700, margin: '4px 0 8px' }}>
                        {booking.hotelName}
                      </h3>
                      <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem' }}>
                        {booking.roomType} × {booking.roomsCount} · {formatDate(booking.checkInDate)} → {formatDate(booking.checkOutDate)}
                        {' · '}{booking.guests?.length || 0} guest{booking.guests?.length === 1 ? '' : 's'}
                      </p>
                    </div>
                    <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
                      <span className={`badge ${badge.className}`}>{badge.label}</span>
                      <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '1.1rem' }}>
                        {formatINR(booking.amount)}
                      </span>
                      {canCompletePayment(booking) && (
                        <button
                          className="btn btn-clay btn-sm"
                          disabled={payingId === booking.id}
                          onClick={() => handlePay(booking)}
                        >
                          {payingId === booking.id ? 'Processing…' : 'Complete payment'}
                        </button>
                      )}
                      {canCancel && (
                        <button
                          className="btn btn-danger btn-sm"
                          disabled={cancellingId === booking.id}
                          onClick={() => handleCancel(booking)}
                        >
                          {cancellingId === booking.id ? 'Cancelling…' : 'Cancel'}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
