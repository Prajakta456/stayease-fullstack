package com.codingshuttle.projects.airBnbApp.strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService(new HolidayCalendar("12-25, 01-26"));

    @Test
    void ordinaryNightFarAhead_isTheBasePrice() {
        assertThat(price(inventory(nextNonHoliday(LocalDate.now().plusDays(60)), 0, 10))).isEqualByComparingTo("1000.00");
    }

    @Test
    void holidaySurcharge_dependsOnTheNightBeingPriced_notOnToday() {
        LocalDate christmas = LocalDate.of(LocalDate.now().getYear() + 1, 12, 25);
        assertThat(price(inventory(christmas, 0, 10))).isEqualByComparingTo("1250.00");
        assertThat(price(inventory(christmas.plusDays(1), 0, 10))).isEqualByComparingTo("1000.00");
    }

    @Test
    void urgencySurcharge_appliesWithinAWeek() {
        LocalDate soon = nextNonHoliday(LocalDate.now().plusDays(2));
        assertThat(price(inventory(soon, 0, 10))).isEqualByComparingTo("1150.00");
    }

    @Test
    void occupancySurcharge_appliesAbove80PercentBooked() {
        LocalDate later = nextNonHoliday(LocalDate.now().plusDays(60));
        assertThat(price(inventory(later, 9, 10))).isEqualByComparingTo("1200.00");
        assertThat(price(inventory(later, 8, 10))).isEqualByComparingTo("1000.00");
    }

    @Test
    void surchargesCompose() {
        LocalDate christmas = LocalDate.of(LocalDate.now().getYear() + 1, 12, 25);
        Inventory inventory = inventory(christmas, 9, 10);
        inventory.setSurgeFactor(new BigDecimal("1.10"));
        // 1000 x 1.10 surge x 1.20 occupancy x 1.25 holiday
        assertThat(price(inventory)).isEqualByComparingTo("1650.00");
    }

    private BigDecimal price(Inventory inventory) {
        return pricingService.calculateDynamicPricing(inventory).setScale(2, RoundingMode.HALF_UP);
    }

    private static Inventory inventory(LocalDate date, int booked, int total) {
        Room room = new Room();
        room.setBasePrice(new BigDecimal("1000"));
        return Inventory.builder()
                .room(room)
                .date(date)
                .bookedCount(booked)
                .reservedCount(0)
                .totalCount(total)
                .surgeFactor(BigDecimal.ONE)
                .build();
    }

    private static LocalDate nextNonHoliday(LocalDate date) {
        HolidayCalendar calendar = new HolidayCalendar("12-25, 01-26");
        while (calendar.isHoliday(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
