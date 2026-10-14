# Airbnb Clone — Full Stack

A full-stack Airbnb-style hotel booking application built with **React** on the frontend and **Spring Boot** on the backend. The app lets users browse hotels, search by date and location, book rooms, and manage their reservations — while admins have a dedicated dashboard to create and manage hotel listings and room inventory.

---

## Project Structure

```
airbnb-fullstack/
├── airbnb-frontend/   # React 18 frontend
└── airBnbApp/         # Spring Boot 3 backend (Java 17)
```

---

## Tech Stack

### Frontend
| Technology | Version |
|---|---|
| React | 18.2 |
| React Router DOM | 6.x |
| Axios | 1.6 |
| React DatePicker | 4.x |
| React Hot Toast | 2.x |
| Lucide React (icons) | 0.303 |
| date-fns | 3.x |

### Backend
| Technology | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.4 |
| Spring Security | 3.4 |
| Spring Data JPA | 3.4 |
| MySQL | 8 |
| Flyway | migrations |
| ModelMapper | 3.2.2 |
| Lombok | latest |

---

## Features

### User-Facing
**Authentication & Profiles** — Secure signup and login flow powered by Spring Security with JWT token authentication. Tokens are stored on the client and sent with every protected request, keeping sessions stateless and scalable.

**Hotel Search & Browse** — Users can search hotels by location, check-in/check-out dates, and guest count. Search results pull live availability from the inventory system and surface the best matching options instantly.

**Hotel Detail Pages** — Each hotel listing shows room types, amenities, pricing, and real-time availability so users can make informed booking decisions before committing.

**Room Booking with Date Selection** — An interactive date picker lets users choose their stay duration. The booking engine checks inventory, calculates the total price, and creates the reservation in a single atomic operation.

**My Bookings Dashboard** — Users can view all their past and upcoming reservations in one place, with booking status tracked throughout the lifecycle (confirmed, cancelled, completed).

### Admin
**Hotel Management** — Admins can create, update, and deactivate hotel listings through a dedicated admin dashboard, including setting contact information and amenities.

**Room & Inventory Management** — Admins control room types, base pricing, and availability windows. The system supports bulk inventory operations so room availability stays accurate across all booking dates.

### Backend Architecture
**Dynamic Pricing Engine** — The backend implements a strategy pattern for pricing, supporting multiple pricing strategies including base pricing, occupancy-based surge, holiday pricing, and urgency pricing — all composable and swappable without changing business logic.

**Global Exception Handling** — A centralized `GlobalExceptionHandler` wraps all API responses in a consistent `ApiResponse` envelope, so the frontend always receives predictable JSON regardless of success or error state.

**Spring Security + JWT** — Signed (HS256), expiring JWTs are validated by `JwtAuthFilter` on every request; logging out revokes all of a user's tokens. Routes are restricted by role (`GUEST` books stays, `HOTEL_MANAGER` manages hotels), and every hotel, room and booking is scoped to its owner. Login and signup are rate-limited per IP and per email.

---

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Java 17+
- MySQL 8 (installed locally, or via Docker)
- Maven is not required: the project ships the Maven wrapper (`./mvnw`)

---

### Backend Setup

1. **Get a MySQL 8 database.** Either:
   - **Local MySQL**: run [`airBnbApp/db/create-local-database.sql`](airBnbApp/db/create-local-database.sql) as an admin user (e.g. in MySQL Workbench) to create the `airbnb` database and an `airbnb_app` user, or
   - **Docker**: `docker compose up -d db` starts MySQL on port **3307** with the defaults from [`docker-compose.yml`](docker-compose.yml).

2. **Create `airBnbApp/.env`** from the example and fill in your database credentials and a JWT secret (at least 32 characters, e.g. `openssl rand -base64 48`). The file is git-ignored.

```bash
cd airBnbApp
cp .env.example .env
```

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/airbnb` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `airbnb_app` / *(none)* |
| `JWT_SECRET` | Token signing key; if unset, a random key is used and restarts sign everyone out | *(random)* |
| `JWT_EXPIRATION_MINUTES` | Token lifetime | `60` |
| `SEED_DEMO_DATA` | Create demo accounts and hotels when the database is empty (local only) | `false` |

The same variables can be set as real environment variables instead (e.g. in production).

3. **Run the backend.** Flyway creates the tables on first start (`src/main/resources/db/migration`).

```bash
./mvnw spring-boot:run
```

With `SEED_DEMO_DATA=true` you can sign in straight away as `guest@demo.com` or `manager@demo.com` (password `password123`).

The backend starts on **http://localhost:8080** with context path `/api/v1`.

---

### Frontend Setup

```bash
cd airbnb-frontend
npm install
npm start
```

The frontend starts on **http://localhost:3000** and proxies API calls to `http://localhost:8080`.

To configure the API URL, copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

---

## API Documentation

Full interactive API documentation is auto-generated by **Swagger UI** and available once the backend is running:

> **Swagger UI:** http://localhost:8080/api/v1/swagger-ui/index.html

You can explore all endpoints, view request/response schemas, and test calls directly from the browser — no external tool needed.

---

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/signup` | Register a new user |
| POST | `/api/v1/auth/login` | Login and receive a JWT token |
| POST | `/api/v1/auth/logout` | Revoke all of the current user's tokens |
| GET | `/api/v1/hotels` | Browse all hotels |
| GET | `/api/v1/hotels/search` | Search hotels by date and criteria |
| GET | `/api/v1/hotels/{id}` | Get hotel details and room info |
| POST | `/api/v1/bookings` | Create a booking |
| GET | `/api/v1/bookings/my` | Get the current user's bookings |
| POST | `/api/v1/admin/hotels` | (Admin) Create a hotel listing |
| POST | `/api/v1/admin/hotels/{id}/rooms` | (Admin) Add a room to a hotel |

---

## License

This project is for educational purposes.
