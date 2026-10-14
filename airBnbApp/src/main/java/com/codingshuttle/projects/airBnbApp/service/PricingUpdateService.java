package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.HotelMinPrice;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.repository.HotelMinPriceRepository;
import com.codingshuttle.projects.airBnbApp.repository.HotelRepository;
import com.codingshuttle.projects.airBnbApp.repository.InventoryRepository;
import com.codingshuttle.projects.airBnbApp.strategy.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingUpdateService {

    // Scheduler to update the inventory and HotelMinPrice tables every hour

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final PricingService pricingService;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 0 * * * *")
    public void updatePrices() {
        int page = 0;
        int batchSize = 100;

        while(true) {
            Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batchSize, Sort.by("id")));
            if(hotelPage.isEmpty()) {
                break;
            }
            // One short transaction per hotel, so row locks taken by the price updates are
            // released quickly instead of blocking bookings for the whole run
            for (Hotel hotel : hotelPage.getContent()) {
                try {
                    transactionTemplate.executeWithoutResult(status -> updateHotelPrices(hotel));
                } catch (Exception e) {
                    log.error("Failed to update prices for hotel ID: {}", hotel.getId(), e);
                }
            }

            page++;
        }
    }

    private void updateHotelPrices(Hotel hotel) {
        log.info("Updating hotel prices for hotel ID: {}", hotel.getId());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);

        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);

        Map<Inventory, BigDecimal> newPrices = updateInventoryPrices(inventoryList);

        updateHotelMinPrice(hotel, newPrices);
    }

    private void updateHotelMinPrice(Hotel hotel, Map<Inventory, BigDecimal> newPrices) {
        // Compute minimum price per day for the hotel
        Map<LocalDate, BigDecimal> dailyMinPrices = new TreeMap<>();
        newPrices.forEach((inventory, price) -> dailyMinPrices.merge(inventory.getDate(), price, BigDecimal::min));

        // Prepare HotelPrice entities in bulk
        List<HotelMinPrice> hotelPrices = new ArrayList<>();
        dailyMinPrices.forEach((date, price) -> {
            HotelMinPrice hotelPrice = hotelMinPriceRepository.findByHotelAndDate(hotel, date)
                    .orElse(new HotelMinPrice(hotel, date));
            hotelPrice.setPrice(price);
            hotelPrices.add(hotelPrice);
        });

        // Save all HotelPrice entities in bulk
        hotelMinPriceRepository.saveAll(hotelPrices);
    }

    /**
     * Writes only the price column. The loaded Inventory entities are deliberately NOT modified:
     * flushing them would rewrite every column, including booked/reserved counts that a booking
     * may have changed since they were read (a lost update).
     */
    private Map<Inventory, BigDecimal> updateInventoryPrices(List<Inventory> inventoryList) {
        Map<Inventory, BigDecimal> newPrices = new LinkedHashMap<>();
        for (Inventory inventory : inventoryList) {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory)
                    .setScale(2, RoundingMode.HALF_UP);
            inventoryRepository.updatePrice(inventory.getId(), dynamicPrice);
            newPrices.put(inventory, dynamicPrice);
        }
        return newPrices;
    }

}
