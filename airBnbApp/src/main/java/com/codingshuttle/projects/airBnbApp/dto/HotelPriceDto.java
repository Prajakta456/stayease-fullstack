package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceDto {
    // NOTE: Hotel entity is used here because JPQL constructor expressions require
    // the exact type. The Hotel entity has @JsonIgnoreProperties on lazy fields.
    private Hotel hotel;
    private Double price;
}
