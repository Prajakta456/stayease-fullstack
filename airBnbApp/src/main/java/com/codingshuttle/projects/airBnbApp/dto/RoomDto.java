package com.codingshuttle.projects.airBnbApp.dto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomDto {
    private Long id;
    private String type;
    private BigDecimal basePrice;
    // ✅ FIXED: was String[] — Room entity uses List<String> with @ElementCollection
    private List<String> photos;
    private List<String> amenities;
    private Integer totalCount;
    private Integer capacity;
}
