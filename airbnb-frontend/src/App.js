import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import './index.css';

import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import SearchResults from './pages/SearchResults';
import HotelDetail from './pages/HotelDetail';
import AdminDashboard from './pages/AdminDashboard';
import RoomAdmin from './pages/RoomAdmin';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import MyBookings from './pages/MyBookings';

function NotFound() {
  return (
    <div style={{ minHeight: '80vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 16 }}>
      <div style={{ fontSize: '5rem' }}>🏚️</div>
      <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '2rem' }}>Page Not Found</h1>
      <a href="/" className="btn btn-clay">Go Home</a>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Navbar />
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchResults />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* Guest-protected routes */}
          <Route
            path="/hotels/:hotelId"
            element={
              <ProtectedRoute requiredRole="GUEST">
                <HotelDetail />
              </ProtectedRoute>
            }
          />
          {/* ✅ FIXED: /bookings route was linked in Navbar but missing from routes */}
          <Route
            path="/bookings"
            element={
              <ProtectedRoute requiredRole="GUEST">
                <MyBookings />
              </ProtectedRoute>
            }
          />

          {/* Hotel Manager-protected routes */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="HOTEL_MANAGER">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/hotels/:hotelId/rooms"
            element={
              <ProtectedRoute requiredRole="HOTEL_MANAGER">
                <RoomAdmin />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<NotFound />} />
        </Routes>
        <Toaster
          position="bottom-right"
          toastOptions={{
            style: {
              background: '#1a1610',
              color: '#ffffff',
              fontFamily: 'DM Sans, sans-serif',
              borderRadius: '12px',
              padding: '12px 20px',
            },
            success: { iconTheme: { primary: '#c8a97e', secondary: '#fff' } },
          }}
        />
      </AuthProvider>
    </BrowserRouter>
  );
}
