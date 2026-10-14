-- Initial schema. Hibernate runs with ddl-auto=validate, so every entity change needs a new
-- migration (V2__..., V3__...) instead of editing this file.

CREATE TABLE app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255),
    password      VARCHAR(255) NOT NULL,
    token_version INTEGER      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles   ENUM ('GUEST', 'HOTEL_MANAGER'),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB;

CREATE TABLE hotel (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255) NOT NULL,
    city         VARCHAR(255),
    active       BIT          NOT NULL,
    owner_id     BIGINT,
    address      VARCHAR(255),
    email        VARCHAR(255),
    location     VARCHAR(255),
    phone_number VARCHAR(255),
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_hotel_owner FOREIGN KEY (owner_id) REFERENCES app_user (id)
) ENGINE = InnoDB;

CREATE TABLE hotel_amenities (
    hotel_id  BIGINT NOT NULL,
    amenities VARCHAR(255),
    CONSTRAINT fk_hotel_amenities_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (id)
) ENGINE = InnoDB;

CREATE TABLE hotel_photos (
    hotel_id BIGINT NOT NULL,
    photos   VARCHAR(255),
    CONSTRAINT fk_hotel_photos_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (id)
) ENGINE = InnoDB;

CREATE TABLE room (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    hotel_id    BIGINT         NOT NULL,
    type        VARCHAR(255)   NOT NULL,
    base_price  DECIMAL(10, 2) NOT NULL,
    total_count INTEGER        NOT NULL,
    capacity    INTEGER        NOT NULL,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (id)
) ENGINE = InnoDB;

CREATE TABLE room_amenities (
    room_id   BIGINT NOT NULL,
    amenities VARCHAR(255),
    CONSTRAINT fk_room_amenities_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE = InnoDB;

CREATE TABLE room_photos (
    room_id BIGINT NOT NULL,
    photos  VARCHAR(255),
    CONSTRAINT fk_room_photos_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE = InnoDB;

-- One row per room type per night
CREATE TABLE inventory (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    hotel_id       BIGINT         NOT NULL,
    room_id        BIGINT         NOT NULL,
    date           DATE           NOT NULL,
    booked_count   INTEGER        NOT NULL DEFAULT 0,
    reserved_count INTEGER        NOT NULL DEFAULT 0,
    total_count    INTEGER        NOT NULL,
    surge_factor   DECIMAL(5, 2)  NOT NULL,
    price          DECIMAL(10, 2) NOT NULL,
    city           VARCHAR(255)   NOT NULL,
    closed         BIT            NOT NULL,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT unique_hotel_room_date UNIQUE (hotel_id, room_id, date),
    CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (id),
    CONSTRAINT fk_inventory_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE = InnoDB;

-- Hotel search filters by city and date range; booking/availability look up a room's nights
CREATE INDEX idx_inventory_city_date ON inventory (city, date);
CREATE INDEX idx_inventory_room_date ON inventory (room_id, date);

CREATE TABLE guest (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    name    VARCHAR(255) NOT NULL,
    gender  ENUM ('FEMALE', 'MALE', 'OTHER'),
    age     INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_guest_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB;

CREATE TABLE booking (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    hotel_id       BIGINT         NOT NULL,
    room_id        BIGINT         NOT NULL,
    user_id        BIGINT         NOT NULL,
    rooms_count    INTEGER        NOT NULL,
    check_in_date  DATE           NOT NULL,
    check_out_date DATE           NOT NULL,
    booking_status ENUM ('CANCELLED', 'CONFIRMED', 'EXPIRED', 'GUESTS_ADDED', 'RESERVED') NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_booking_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (id),
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB;

-- The expiry job scans unpaid holds by status and age; "My Bookings" lists a user's newest first
CREATE INDEX idx_booking_status_created ON booking (booking_status, created_at);
CREATE INDEX idx_booking_user_created ON booking (user_id, created_at);

CREATE TABLE booking_guest (
    booking_id BIGINT NOT NULL,
    guest_id   BIGINT NOT NULL,
    PRIMARY KEY (booking_id, guest_id),
    CONSTRAINT fk_booking_guest_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_booking_guest_guest FOREIGN KEY (guest_id) REFERENCES guest (id)
) ENGINE = InnoDB;

CREATE TABLE payment (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    booking_id     BIGINT,
    transaction_id VARCHAR(255)   NOT NULL,
    payment_status ENUM ('CANCELLED', 'CONFIRMED', 'PENDING') NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_booking UNIQUE (booking_id),
    CONSTRAINT uk_payment_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
) ENGINE = InnoDB;
