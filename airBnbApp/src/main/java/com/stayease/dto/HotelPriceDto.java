package com.stayease.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceDto {
    private HotelDto hotel;
    // Average nightly price of the cheapest room type bookable for the whole stay
    private BigDecimal price;
}
