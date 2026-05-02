# StayEase — AirBnb App Frontend

A complete React frontend for the Spring Boot AirBnb application.

## Tech Stack
- React 18 + React Router v6
- Axios (API calls)
- react-hot-toast (notifications)
- Custom CSS design system (no UI framework)

## Project Structure

```
src/
├── components/
│   └── Navbar.jsx          # Sticky top navigation
├── pages/
│   ├── HomePage.jsx         # Hero + search form
│   ├── SearchResults.jsx    # Hotel search results grid
│   ├── HotelDetail.jsx      # Hotel info + 3-step booking flow
│   ├── AdminDashboard.jsx   # Admin: create/edit/delete hotels
│   └── RoomAdmin.jsx        # Admin: manage rooms per hotel
├── services/
│   └── api.js              # All API calls (mirrors backend controllers)
├── App.js                   # Router
├── index.css                # Full design system & CSS
└── index.js                 # Entry point
```

## API Mapping

| Frontend Page       | Backend Controller           | Endpoints Used |
|---------------------|------------------------------|----------------|
| HomePage            | -                            | -              |
| SearchResults       | HotelBrowseController        | POST /hotels/search |
| HotelDetail (view)  | HotelBrowseController        | GET /hotels/{id}/info |
| HotelDetail (book)  | HotelBookingController       | POST /bookings/init, POST /bookings/{id}/addGuests |
| AdminDashboard      | HotelController              | POST/GET/PUT/DELETE/PATCH /admin/hotels/** |
| RoomAdmin           | RoomAdminController          | POST/GET/DELETE /admin/hotels/{id}/rooms/** |

## Setup & Run

### 1. Start the Spring Boot backend
```bash
cd airBnbApp
./mvnw spring-boot:run
```
Backend runs on `http://localhost:8080`

### 2. Install frontend dependencies
```bash
cd airbnb-frontend
npm install
```

### 3. Configure API URL (optional)
```bash
cp .env.example .env
# Edit .env if your backend runs on a different port
```

### 4. Start the frontend
```bash
npm start
```
Frontend runs on `http://localhost:3000`

## Pages & Features

### 🏠 Home Page (`/`)
- Full-screen hero with animated search box
- Search by city, check-in/out dates, rooms count
- Quick-select popular cities

### 🔍 Search Results (`/search`)
- Grid of hotel cards with pricing
- Filter bar to update search params
- Pagination support

### 🏨 Hotel Detail (`/hotels/:id`)
- Hotel hero with amenities
- Room selection cards with pricing
- 3-step booking wizard:
  1. Select room + dates
  2. Add guest details
  3. Booking confirmation
- Live price summary sidebar

### 🔧 Admin Dashboard (`/admin`)
- Create / Edit / Delete hotels
- Activate inactive hotels
- Lookup hotel by ID
- Navigate to room management

### 🛏️ Room Admin (`/admin/hotels/:id/rooms`)
- View all rooms for a hotel
- Create rooms with type, price, capacity, amenities
- Delete rooms

## CORS Note
If the backend doesn't have CORS configured, add this to your Spring Boot app:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("*")
            .allowedHeaders("*");
    }
}
```
