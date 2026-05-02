package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.HotelContactInfo;
import lombok.Data;

import java.util.List;

@Data
public class HotelDto {
    private Long id;
    private String name;
    private String city;
    // ✅ FIXED: was String[] — entity uses List<String>, must match to avoid ModelMapper mismatch
    private List<String> photos;
    private List<String> amenities;
    private HotelContactInfo contactInfo;
    private Boolean active;
}
