# StayEase — Frontend

React single-page app for the StayEase hotel booking platform. See the [root README](../README.md) for the full setup, including the backend.

## Tech stack
- React 18 + React Router 6 (Create React App)
- Axios for API calls (`src/services/api.js`)
- react-hot-toast for notifications
- Hand-written CSS design system in `src/index.css` (no UI framework)

## Run

```bash
npm install
npm start        # http://localhost:3000
npm test         # unit tests
npm run build    # production build in build/
```

The backend must be running on `http://localhost:8080/api/v1`, or set `REACT_APP_API_URL` in a `.env` file (see `.env.example`). Variables are baked in at build time.

## Structure

```
src/
├── App.js                   # Routes
├── context/AuthContext.jsx  # Signed-in user; signs out when the API reports an expired or revoked token
├── components/
│   ├── Navbar.jsx           # Navigation and account menu
│   └── ProtectedRoute.jsx   # Role-gated routes; redirects to sign-in and back
├── pages/
│   ├── HomePage.jsx         # Hero and search form
│   ├── SearchResults.jsx    # Results grid; filters and page live in the URL
│   ├── HotelDetail.jsx      # Rooms with live price/availability; reserve → guests → pay → confirmed
│   ├── MyBookings.jsx       # Guest's bookings: cancel or finish paying
│   ├── LoginPage.jsx / SignupPage.jsx
│   ├── AdminDashboard.jsx   # Manager: create, edit, activate/deactivate, delete hotels
│   └── RoomAdmin.jsx        # Manager: add and delete room types
├── services/api.js          # All backend calls, the auth header and error-message helper
└── utils/format.js          # Local-date and price helpers (with tests)
```

## Routes

| Path | Access | Backend endpoints |
|---|---|---|
| `/` | Public | — |
| `/search` | Public | `POST /hotels/search` |
| `/hotels/:id` | Public (reserving needs a guest account) | `GET /hotels/{id}/info`, `POST /bookings/init`, `/addGuests`, `/payments`, `/cancel` |
| `/bookings` | Guest | `GET /bookings`, `/payments`, `/cancel` |
| `/admin` | Hotel manager | `/admin/hotels/**` |
| `/admin/hotels/:id/rooms` | Hotel manager | `/admin/hotels/{id}/rooms/**` |
| `/login`, `/signup` | Public | `POST /auth/login`, `POST /auth/signup` |

Dates are always handled as local `YYYY-MM-DD` strings (see `utils/format.js`), never via `toISOString()`, which would shift them to UTC.
