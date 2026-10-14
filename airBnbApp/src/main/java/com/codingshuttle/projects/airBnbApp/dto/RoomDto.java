package com.codingshuttle.projects.airBnbApp.dto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomDto {
    private Long id;
    private String type;
    private BigDecimal basePrice;
    private List<String> photos;
    private List<String> amenities;
    private Integer totalCount;
    private Integer capacity;

    // Only filled in by GET /hotels/{id}/info when stay dates are given:
    // the average nightly price for those dates, and whether the room can be booked for all of them
    private BigDecimal price;
    private Boolean available;
}
