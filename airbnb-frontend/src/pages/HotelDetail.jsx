import { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { getHotelInfo, initialiseBooking, addGuests, payForBooking, cancelBooking, getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { todayISO, addDaysISO, nightsBetween, formatINR } from '../utils/format';
import toast from 'react-hot-toast';

const STEP_LABELS = ['Select Room', 'Guest Details', 'Payment', 'Confirmed'];

const formatTime = (iso) => new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });

function StepIndicator({ current }) {
  return (
    <div className="step-indicator">
      {STEP_LABELS.map((label, i) => (
        <div key={i} className={`step ${i < current ? 'done' : i === current ? 'active' : ''}`}>
          <div className="step-circle">
            {i < current ? '✓' : i + 1}
          </div>
          <span className="step-label">{label}</span>
        </div>
      ))}
    </div>
  );
}

function GuestForm({ index, guest, onChange }) {
  return (
    <div style={{ background: 'var(--ash)', borderRadius: 'var(--radius-sm)', padding: 20, marginBottom: 16 }}>
      <p style={{ fontWeight: 600, marginBottom: 16, fontSize: '0.9rem' }}>Guest {index + 1}</p>
      <div className="form-grid-2">
        <div className="form-group" style={{ gridColumn: '1 / -1' }}>
          <label className="form-label">Full Name</label>
          <input
            className="form-input"
            placeholder="John Doe"
            value={guest.name}
            onChange={e => onChange(index, 'name', e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label className="form-label">Gender</label>
          <select
            className="form-input"
            value={guest.gender}
            onChange={e => onChange(index, 'gender', e.target.value)}
          >
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">Age</label>
          <input
            className="form-input"
            type="number"
            placeholder="25"
            min="1"
            max="120"
            value={guest.age}
            onChange={e => onChange(index, 'age', +e.target.value)}
            required
          />
        </div>
      </div>
    </div>
  );
}

export default function HotelDetail() {
  const { hotelId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isGuest } = useAuth();
  const state = location.state || {};

  const [info, setInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [step, setStep] = useState(0);
  const [booking, setBooking] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const today = todayISO();

  const [checkIn, setCheckIn] = useState(state.startDate || today);
  const [checkOut, setCheckOut] = useState(state.endDate || addDaysISO(state.startDate || today, 1));
  const [roomsCount, setRoomsCount] = useState(state.roomsCount || 1);

  const nights = nightsBetween(checkIn, checkOut);

  // Moving check-in past check-out pushes check-out to the next day
  const handleCheckInChange = (value) => {
    setCheckIn(value);
    if (value && nightsBetween(value, checkOut) === 0) {
      setCheckOut(addDaysISO(value, 1));
    }
  };

  const [guests, setGuests] = useState([{ name: '', gender: 'MALE', age: '' }]);

  const validDates = nights > 0;

  // Re-fetch when the stay changes so each room shows its real price and availability for those nights
  useEffect(() => {
    const stay = validDates ? { startDate: checkIn, endDate: checkOut, roomsCount } : undefined;
    getHotelInfo(hotelId, stay)
      .then(data => {
        setInfo(data);
        setLoadError(null);
        // Drop a selection that is no longer bookable for the new dates
        setSelectedRoom(current => {
          const fresh = current && data.rooms?.find(r => r.id === current.id);
          return fresh && fresh.available !== false ? fresh : null;
        });
      })
      .catch(e => setLoadError(e.response?.status === 404
        ? 'This hotel does not exist or is not open for bookings.'
        : getErrorMessage(e, 'Could not load this hotel. Please try again.')))
      .finally(() => setLoading(false));
  }, [hotelId, checkIn, checkOut, roomsCount, validDates]);

  const nightlyPrice = (room) => Number(room.price ?? room.basePrice);

  const handleGuestChange = (idx, field, val) => {
    setGuests(g => g.map((guest, i) => i === idx ? { ...guest, [field]: val } : guest));
  };

  const addGuestSlot = () => setGuests(g => [...g, { name: '', gender: 'MALE', age: '' }]);
  const removeGuestSlot = (idx) => setGuests(g => g.filter((_, i) => i !== idx));

  const handleInitBooking = async () => {
    if (!selectedRoom) return toast.error('Please select a room');
    if (!user) {
      // Browsing is public; come back to this hotel with the same stay after signing in
      return navigate('/login', {
        state: { from: location.pathname, fromState: { startDate: checkIn, endDate: checkOut, roomsCount } },
      });
    }
    if (!isGuest) return toast.error('Sign in with a guest account to book a stay');
    setSubmitting(true);
    try {
      const b = await initialiseBooking({
        hotelId: parseInt(hotelId),
        roomId: selectedRoom.id,
        checkInDate: checkIn,
        checkOutDate: checkOut,
        roomsCount,
      });
      setBooking(b);
      setStep(1);
    } catch (e) {
      toast.error(getErrorMessage(e, 'Booking failed. Try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  const maxGuests = selectedRoom ? selectedRoom.capacity * roomsCount : 1;

  const handleAddGuests = async () => {
    const valid = guests.every(g => g.name && g.age > 0);
    if (!valid) return toast.error('Please fill all guest details');
    if (guests.length > maxGuests) return toast.error(`This booking allows at most ${maxGuests} guests`);
    setSubmitting(true);
    try {
      const updated = await addGuests(booking.id, guests);
      setBooking(updated);
      setStep(2);
    } catch (e) {
      toast.error(getErrorMessage(e, 'Could not add guests'));
    } finally {
      setSubmitting(false);
    }
  };

  const handlePay = async () => {
    setSubmitting(true);
    try {
      const confirmed = await payForBooking(booking.id);
      setBooking(confirmed);
      setStep(3);
    } catch (e) {
      toast.error(getErrorMessage(e, 'Payment failed. Try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  // Going back to change room/dates releases the current hold instead of leaving it reserved
  const handleChangeSelection = async () => {
    setSubmitting(true);
    try {
      await cancelBooking(booking.id);
    } catch {
      // Already expired or cancelled — nothing is held any more
    } finally {
      setBooking(null);
      setStep(0);
      setSubmitting(false);
    }
  };

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;
  if (!info) return (
    <div className="container" style={{ padding: '80px 24px', textAlign: 'center' }}>
      <h2>{loadError || 'Hotel not found'}</h2>
      <button className="btn btn-clay" onClick={() => navigate(-1)} style={{ marginTop: 16 }}>Go Back</button>
    </div>
  );

  const { hotel, rooms } = info;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--ash)' }}>
      {/* Hotel hero */}
      <div style={{
        background: 'var(--ink)',
        minHeight: 320,
        display: 'flex',
        alignItems: 'flex-end',
        padding: '0 24px 40px',
        position: 'relative',
        overflow: 'hidden',
      }}>
        <div style={{
          position: 'absolute', inset: 0,
          background: 'radial-gradient(ellipse 80% 60% at 50% 50%, rgba(200,169,126,0.15) 0%, transparent 70%)',
        }} />
        <div style={{
          position: 'absolute', inset: 0,
          backgroundImage: 'linear-gradient(rgba(200,169,126,0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(200,169,126,0.05) 1px, transparent 1px)',
          backgroundSize: '40px 40px',
        }} />
        <div className="container" style={{ position: 'relative', zIndex: 1 }}>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => navigate(-1)}
            style={{ marginBottom: 24, background: 'rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.8)' }}
          >
            ← Back
          </button>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 24, flexWrap: 'wrap' }}>
            <div>
              <div style={{ color: 'var(--clay)', fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 8 }}>
                {hotel.city}
              </div>
              <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2.8rem', fontWeight: 900, color: 'var(--white)', letterSpacing: '-1px', marginBottom: 12 }}>
                {hotel.name}
              </h1>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {(hotel.amenities || []).map((a, i) => (
                  <span key={i} style={{
                    background: 'rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.75)',
                    padding: '4px 12px', borderRadius: '100px', fontSize: '0.78rem'
                  }}>{a}</span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="container" style={{ padding: '40px 24px' }}>
        <div className="detail-layout">

          {/* Left: Room selection / steps */}
          <div>
            <StepIndicator current={step} />

            {/* Step 0: Select Room */}
            {step === 0 && (
              <div>
                {/* Date/rooms picker */}
                <div className="card" style={{ marginBottom: 24 }}>
                  <div className="card-body">
                    <h2 className="section-title" style={{ marginBottom: 20 }}>Your Stay Details</h2>
                    <div className="form-grid-3">
                      <div className="form-group">
                        <label className="form-label">Check-in</label>
                        <input className="form-input" type="date" value={checkIn} min={today} onChange={e => handleCheckInChange(e.target.value)} />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Check-out</label>
                        <input className="form-input" type="date" value={checkOut} min={addDaysISO(checkIn, 1)} onChange={e => setCheckOut(e.target.value)} />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Rooms</label>
                        <select className="form-input" value={roomsCount} onChange={e => setRoomsCount(+e.target.value)}>
                          {[1,2,3,4,5].map(n => <option key={n} value={n}>{n}</option>)}
                        </select>
                      </div>
                    </div>
                  </div>
                </div>

                <h2 className="section-title" style={{ marginBottom: 16 }}>Available Rooms</h2>
                {(!rooms || rooms.length === 0) ? (
                  <div className="empty-state">
                    <div className="empty-state-icon">🛏️</div>
                    <h3>No rooms available</h3>
                    <p>Try different dates or rooms count.</p>
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    {rooms.map(room => {
                      const unavailable = room.available === false;
                      return (
                      <div
                        key={room.id}
                        className={`room-card ${selectedRoom?.id === room.id ? 'selected' : ''}`}
                        onClick={() => !unavailable && setSelectedRoom(room)}
                        style={unavailable ? { opacity: 0.5, cursor: 'not-allowed' } : undefined}
                      >
                        <div className="room-card-info">
                          <h3>{room.type}</h3>
                          <div className="room-card-meta">
                            <span>👥 Capacity: {room.capacity}</span>
                            <span>🏠 Total: {room.totalCount} rooms</span>
                          </div>
                          {room.amenities && room.amenities.length > 0 && (
                            <div style={{ marginTop: 10, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                              {room.amenities.map((a, i) => (
                                <span key={i} className="amenity-tag">{a}</span>
                              ))}
                            </div>
                          )}
                        </div>
                        <div className="room-card-price">
                          <div className="amount">{formatINR(nightlyPrice(room))}</div>
                          <div className="per-night">{room.price != null ? 'avg. per night for your dates' : 'per night'}</div>
                          <div style={{ marginTop: 8 }}>
                            {unavailable ? (
                              <span className="badge badge-gray">Not available for these dates</span>
                            ) : selectedRoom?.id === room.id ? (
                              <span className="badge badge-clay">✓ Selected</span>
                            ) : (
                              <span style={{ color: 'var(--clay)', fontSize: '0.82rem', fontWeight: 600, cursor: 'pointer' }}>Select →</span>
                            )}
                          </div>
                        </div>
                      </div>
                      );
                    })}
                  </div>
                )}

                {selectedRoom && (
                  <div style={{ marginTop: 24 }}>
                    <button
                      className="btn btn-clay btn-lg w-full"
                      onClick={handleInitBooking}
                      disabled={submitting || !validDates}
                    >
                      {submitting ? 'Reserving...' : user ? 'Reserve Room →' : 'Sign in to reserve →'}
                    </button>
                    {!validDates && (
                      <p style={{ color: 'var(--red)', fontSize: '0.85rem', marginTop: 8 }}>
                        Check-out must be after check-in.
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Step 1: Guest Details */}
            {step === 1 && (
              <div>
                <div className="card" style={{ marginBottom: 20 }}>
                  <div className="card-body">
                    <p style={{ color: 'var(--green)', fontWeight: 600, marginBottom: 4 }}>✓ Room Reserved</p>
                    <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem' }}>
                      Booking #{booking?.id} — held for you until {booking?.holdExpiresAt ? formatTime(booking.holdExpiresAt) : 'soon'}.
                      Complete payment before then to confirm it.
                    </p>
                  </div>
                </div>

                <h2 className="section-title" style={{ marginBottom: 8 }}>Guest Information</h2>
                <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem', marginBottom: 24 }}>
                  Add details for all guests staying in this booking (up to {maxGuests}).
                </p>

                {guests.map((guest, i) => (
                  <div key={i} style={{ position: 'relative' }}>
                    <GuestForm index={i} guest={guest} onChange={handleGuestChange} />
                    {guests.length > 1 && (
                      <button
                        onClick={() => removeGuestSlot(i)}
                        style={{
                          position: 'absolute', top: 16, right: 16,
                          background: 'var(--red)', color: 'white',
                          border: 'none', borderRadius: '50%', width: 28, height: 28,
                          cursor: 'pointer', fontSize: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'center'
                        }}
                      >×</button>
                    )}
                  </div>
                ))}

                {guests.length < maxGuests && (
                  <button className="btn btn-ghost" onClick={addGuestSlot} style={{ marginBottom: 20 }}>
                    + Add Guest
                  </button>
                )}

                <div style={{ display: 'flex', gap: 12 }}>
                  <button className="btn btn-outline" onClick={handleChangeSelection} disabled={submitting}>
                    ← Change room or dates
                  </button>
                  <button
                    className="btn btn-clay"
                    style={{ flex: 1 }}
                    onClick={handleAddGuests}
                    disabled={submitting}
                  >
                    {submitting ? 'Saving...' : 'Continue →'}
                  </button>
                </div>
              </div>
            )}

            {/* Step 2: Payment */}
            {step === 2 && booking && (
              <div>
                <div className="card" style={{ marginBottom: 20 }}>
                  <div className="card-body">
                    <h2 className="section-title" style={{ marginBottom: 8 }}>Review & Pay</h2>
                    <p style={{ color: 'var(--ink-mid)', fontSize: '0.88rem', marginBottom: 20 }}>
                      Your rooms are held until {booking.holdExpiresAt ? formatTime(booking.holdExpiresAt) : 'soon'}.
                    </p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                      {[
                        ['Room', `${booking.roomType} × ${booking.roomsCount}`],
                        ['Check-in', booking.checkInDate],
                        ['Check-out', booking.checkOutDate],
                        ['Guests', booking.guests?.length || 0],
                      ].map(([label, val]) => (
                        <div key={label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                          <span style={{ color: 'var(--ink-mid)' }}>{label}</span>
                          <span style={{ fontWeight: 600 }}>{val}</span>
                        </div>
                      ))}
                      <div style={{ height: 1, background: 'var(--ash-dark)' }} />
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ fontWeight: 700 }}>Total to pay</span>
                        <span style={{ fontFamily: 'var(--font-display)', fontSize: '1.4rem', fontWeight: 800, color: 'var(--clay-dark)' }}>
                          {formatINR(booking.amount)}
                        </span>
                      </div>
                    </div>
                    <p style={{ fontSize: '0.75rem', color: 'var(--ink-mid)', marginTop: 16 }}>
                      Demo mode: payments use a mock gateway and no money is charged.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 12 }}>
                  <button className="btn btn-outline" onClick={handleChangeSelection} disabled={submitting}>
                    ← Change room or dates
                  </button>
                  <button className="btn btn-clay" style={{ flex: 1 }} onClick={handlePay} disabled={submitting}>
                    {submitting ? 'Processing...' : `Pay ${formatINR(booking.amount)} →`}
                  </button>
                </div>
              </div>
            )}

            {/* Step 3: Confirmation */}
            {step === 3 && (
              <div style={{ textAlign: 'center', padding: '40px 0' }}>
                <div style={{ fontSize: '4rem', marginBottom: 20 }}>🎉</div>
                <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '2rem', fontWeight: 800, marginBottom: 12 }}>
                  Booking Confirmed!
                </h2>
                <p style={{ color: 'var(--ink-mid)', marginBottom: 32 }}>
                  Your stay at <strong>{hotel.name}</strong> is booked and paid.
                </p>

                {booking && (
                  <div className="card" style={{ textAlign: 'left', marginBottom: 32 }}>
                    <div className="card-body">
                      <h3 style={{ fontFamily: 'var(--font-display)', marginBottom: 20 }}>Booking Summary</h3>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {[
                          ['Booking ID', `#${booking.id}`],
                          ['Status', booking.bookingStatus],
                          ['Check-in', booking.checkInDate],
                          ['Check-out', booking.checkOutDate],
                          ['Rooms', booking.roomsCount],
                          ['Guests', booking.guests?.length || 0],
                          ['Amount paid', formatINR(booking.amount)],
                        ].map(([label, val]) => (
                          <div key={label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                            <span style={{ color: 'var(--ink-mid)' }}>{label}</span>
                            <span style={{ fontWeight: 600 }}>{val}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}

                <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
                  <button className="btn btn-outline" onClick={() => navigate('/')}>← Explore More</button>
                  <button className="btn btn-clay" onClick={() => navigate('/bookings')}>My Bookings</button>
                </div>
              </div>
            )}
          </div>

          {/* Right: Price summary sidebar */}
          {step < 3 && (
            <div style={{ position: 'sticky', top: 88 }}>
              <div className="card">
                <div className="card-body">
                  <h3 style={{ fontFamily: 'var(--font-display)', fontSize: '1.2rem', fontWeight: 700, marginBottom: 20 }}>
                    Price Summary
                  </h3>

                  {selectedRoom ? (
                    <>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 20 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                          <span style={{ color: 'var(--ink-mid)' }}>Room type</span>
                          <span style={{ fontWeight: 600 }}>{selectedRoom.type}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                          <span style={{ color: 'var(--ink-mid)' }}>Nightly price</span>
                          <span>{formatINR(nightlyPrice(selectedRoom))}/night</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                          <span style={{ color: 'var(--ink-mid)' }}>Nights</span>
                          <span>{nights}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                          <span style={{ color: 'var(--ink-mid)' }}>Rooms</span>
                          <span>× {roomsCount}</span>
                        </div>
                        <div style={{ height: 1, background: 'var(--ash-dark)' }} />
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span style={{ fontWeight: 700 }}>{booking ? 'Total' : 'Estimated total'}</span>
                          <span style={{ fontFamily: 'var(--font-display)', fontSize: '1.4rem', fontWeight: 800, color: 'var(--clay-dark)' }}>
                            {formatINR(booking ? booking.amount : nightlyPrice(selectedRoom) * nights * roomsCount)}
                          </span>
                        </div>
                      </div>
                      <p style={{ fontSize: '0.75rem', color: 'var(--ink-mid)', textAlign: 'center' }}>
                        {booking
                          ? 'Final price for your dates, including demand-based pricing'
                          : 'Nightly prices vary with demand; your exact total is shown once the room is reserved'}
                      </p>
                    </>
                  ) : (
                    <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--ink-mid)' }}>
                      <div style={{ fontSize: '2rem', marginBottom: 8 }}>🛏️</div>
                      <p style={{ fontSize: '0.88rem' }}>Select a room to see pricing</p>
                    </div>
                  )}
                </div>
              </div>

              {/* Contact info */}
              {hotel.contactInfo && (
                <div className="card" style={{ marginTop: 16 }}>
                  <div className="card-body">
                    <h3 style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 12, color: 'var(--ink-mid)' }}>
                      Contact
                    </h3>
                    {hotel.contactInfo.phoneNumber && (
                      <div style={{ fontSize: '0.88rem', marginBottom: 6 }}>📞 {hotel.contactInfo.phoneNumber}</div>
                    )}
                    {hotel.contactInfo.email && (
                      <div style={{ fontSize: '0.88rem', marginBottom: 6 }}>✉️ {hotel.contactInfo.email}</div>
                    )}
                    {hotel.contactInfo.address && (
                      <div style={{ fontSize: '0.88rem' }}>📍 {hotel.contactInfo.address}</div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
