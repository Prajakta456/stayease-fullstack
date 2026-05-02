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

// ---- Auth ----
export const signup = (data) =>
  api.post('/auth/signup', data).then(r => r.data?.data || r.data);

export const loginApi = (data) =>
  api.post('/auth/login', data).then(r => r.data?.data || r.data);

// ---- Hotel Browse (Public) ----
export const searchHotels = (searchRequest) =>
  api.post('/hotels/search', searchRequest).then(r => r.data?.data || r.data);

export const getHotelInfo = (hotelId) =>
  api.get(`/hotels/${hotelId}/info`).then(r => r.data?.data || r.data);

// ---- Hotel Admin ----
export const createHotel = (hotelDto) =>
  api.post('/admin/hotels', hotelDto).then(r => r.data?.data || r.data);

// ✅ NEW: fetch all hotels for the admin dashboard list
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
export const initialiseBooking = (bookingRequest) =>
  api.post('/bookings/init', bookingRequest).then(r => r.data?.data || r.data);

export const addGuests = (bookingId, guests) =>
  api.post(`/bookings/${bookingId}/addGuests`, guests).then(r => r.data?.data || r.data);

export default api;
