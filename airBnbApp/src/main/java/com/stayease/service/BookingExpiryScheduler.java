package com.stayease.service;

import com.stayease.entity.enums.BookingStatus;
import com.stayease.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Releases rooms held by bookings that were never paid for. Without this, every abandoned
 * checkout would keep its rooms reserved forever.
 */
@Component
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final Duration holdDuration;

    public BookingExpiryScheduler(BookingRepository bookingRepository,
                                  BookingService bookingService,
                                  @Value("${booking.hold-minutes:10}") long holdMinutes) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.holdDuration = Duration.ofMinutes(holdMinutes);
    }

    @Scheduled(fixedDelayString = "${booking.expiry-check-interval-ms:60000}")
    public void expireElapsedHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minus(holdDuration);
        List<Long> candidates = bookingRepository.findIdsByStatusInAndCreatedAtBefore(
                EnumSet.of(BookingStatus.RESERVED, BookingStatus.GUESTS_ADDED), cutoff);

        int expired = 0;
        for (Long bookingId : candidates) {
            try {
                // Each booking in its own transaction, so one failure doesn't block the rest
                if (bookingService.expireIfHoldElapsed(bookingId)) {
                    expired++;
                }
            } catch (Exception e) {
                log.error("Failed to expire booking {}", bookingId, e);
            }
        }
        if (expired > 0) {
            log.info("Expired {} unpaid booking(s)", expired);
        }
    }
}
