package com.codingshuttle.projects.airBnbApp.repository;

import java.math.BigDecimal;

/** One row of the hotel search: a hotel and the cheapest average nightly price among its bookable rooms. */
public interface HotelPriceProjection {
    Long getHotelId();

    BigDecimal getPrice();
}
