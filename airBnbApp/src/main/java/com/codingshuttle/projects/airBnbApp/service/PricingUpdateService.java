package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
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
import java.util.List;

/** Hourly job that recomputes every inventory row's nightly price with the dynamic pricing strategies. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingUpdateService {

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
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

        // Writes only the price column. The loaded entities are deliberately NOT modified: flushing
        // them would rewrite every column, including booked/reserved counts that a booking may have
        // changed since they were read (a lost update).
        for (Inventory inventory : inventoryList) {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory)
                    .setScale(2, RoundingMode.HALF_UP);
            inventoryRepository.updatePrice(inventory.getId(), dynamicPrice);
        }
    }
}
