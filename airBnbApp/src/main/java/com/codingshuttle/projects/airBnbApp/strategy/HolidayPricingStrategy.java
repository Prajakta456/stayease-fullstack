package com.codingshuttle.projects.airBnbApp.strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy{

    private static final BigDecimal HOLIDAY_MULTIPLIER = BigDecimal.valueOf(1.25);

    private final PricingStrategy wrapped;
    private final HolidayCalendar holidayCalendar;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        // The night being priced, not today's date, decides the surcharge
        if (holidayCalendar.isHoliday(inventory.getDate())) {
            price = price.multiply(HOLIDAY_MULTIPLIER);
        }
        return price;
    }
}
