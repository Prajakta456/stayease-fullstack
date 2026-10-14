import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// Attach token from localStorage on every request
api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('airbnb_user');
  if (stored) {
    try {
      const user = JSON.parse(stored);
      if (user?.token) {
        config.headers['Authorization'] = `Bearer ${user.token}`;
      }
    } catch {}
  }
  return config;
});

// A 401 on an authenticated request means the session expired or was revoked:
// drop the stored user and let AuthContext send the user back to sign in.
export const SESSION_EXPIRED_EVENT = 'auth:session-expired';
const AUTH_ENDPOINTS = ['/auth/login', '/auth/signup', '/auth/logout'];

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = AUTH_ENDPOINTS.some(path => error.config?.url?.endsWith(path));
    if (error.response?.status === 401 && !isAuthEndpoint && localStorage.getItem('airbnb_user')) {
      localStorage.removeItem('airbnb_user');
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    return Promise.reject(error);
  }
);

// ---- Auth ----
export const signup = (data) =>
  api.post('/auth/signup', data).then(r => r.data?.data || r.data);

export const loginApi = (data) =>
  api.post('/auth/login', data).then(r => r.data?.data || r.data);

// Revokes every token issued to the current user
export const logoutApi = () => api.post('/auth/logout');

// ---- Hotel Browse (Public) ----
export const searchHotels = (searchRequest) =>
  api.post('/hotels/search', searchRequest).then(r => r.data?.data || r.data);

// With { startDate, endDate, roomsCount }, each room also carries `price` (avg nightly) and `available`
export const getHotelInfo = (hotelId, stay) =>
  api.get(`/hotels/${hotelId}/info`, { params: stay }).then(r => r.data?.data || r.data);

// ---- Hotel Admin ----
export const createHotel = (hotelDto) =>
  api.post('/admin/hotels', hotelDto).then(r => r.data?.data || r.data);

// The signed-in manager's hotels
export const getAllHotels = () =>
  api.get('/admin/hotels').then(r => r.data?.data || r.data);

export const getHotelById = (hotelId) =>
  api.get(`/admin/hotels/${hotelId}`).then(r => r.data?.data || r.data);

export const updateHotel = (hotelId, hotelDto) =>
  api.put(`/admin/hotels/${hotelId}`, hotelDto).then(r => r.data?.data || r.data);

export const deleteHotel = (hotelId) =>
  api.delete(`/admin/hotels/${hotelId}`).then(r => r.data?.data || r.data);

export const activateHotel = (hotelId) =>
  api.patch(`/admin/hotels/${hotelId}/activate`).then(r => r.data?.data || r.data);

export const deactivateHotel = (hotelId) =>
  api.patch(`/admin/hotels/${hotelId}/deactivate`).then(r => r.data?.data || r.data);

// ---- Room Admin ----
export const createRoom = (hotelId, roomDto) =>
  api.post(`/admin/hotels/${hotelId}/rooms`, roomDto).then(r => r.data?.data || r.data);

export const getAllRooms = (hotelId) =>
  api.get(`/admin/hotels/${hotelId}/rooms`).then(r => r.data?.data || r.data);

export const getRoomById = (hotelId, roomId) =>
  api.get(`/admin/hotels/${hotelId}/rooms/${roomId}`).then(r => r.data?.data || r.data);

export const deleteRoom = (hotelId, roomId) =>
  api.delete(`/admin/hotels/${hotelId}/rooms/${roomId}`).then(r => r.data?.data || r.data);

// ---- Bookings ----
export const getMyBookings = () =>
  api.get('/bookings').then(r => r.data?.data || r.data);

export const initialiseBooking = (bookingRequest) =>
  api.post('/bookings/init', bookingRequest).then(r => r.data?.data || r.data);

export const addGuests = (bookingId, guests) =>
  api.post(`/bookings/${bookingId}/addGuests`, guests).then(r => r.data?.data || r.data);

export const payForBooking = (bookingId) =>
  api.post(`/bookings/${bookingId}/payments`).then(r => r.data?.data || r.data);

export const cancelBooking = (bookingId) =>
  api.post(`/bookings/${bookingId}/cancel`).then(r => r.data?.data || r.data);

// The backend wraps errors as { error: { message, subErrors } }
export const getErrorMessage = (err, fallback) =>
  err?.response?.data?.error?.message || fallback;

export default api;
