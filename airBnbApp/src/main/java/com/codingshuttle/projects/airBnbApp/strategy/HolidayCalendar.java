package com.codingshuttle.projects.airBnbApp.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recurring holidays (same day every year) configured as {@code pricing.holidays=MM-dd,MM-dd,...}.
 * Holidays on moving dates (Diwali, Holi, ...) can be added per year the same way.
 */
@Component
public class HolidayCalendar {

    private final Set<MonthDay> holidays;

    public HolidayCalendar(@Value("${pricing.holidays:}") String holidays) {
        this.holidays = Arrays.stream(holidays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> MonthDay.parse("--" + s))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(MonthDay.from(date));
    }
}
