package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.HotelInfoDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelSearchRequest;
import com.codingshuttle.projects.airBnbApp.service.HotelService;
import com.codingshuttle.projects.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelBrowseController {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    // ✅ FIXED: was @GetMapping but frontend sends POST with a body
    @PostMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {
        var page = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
}
