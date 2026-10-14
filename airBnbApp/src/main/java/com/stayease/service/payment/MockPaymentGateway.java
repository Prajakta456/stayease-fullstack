package com.stayease.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Demo gateway: every charge succeeds and no real money moves. */
@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String charge(Long bookingId, BigDecimal amount) {
        String transactionId = "mock_" + UUID.randomUUID();
        log.info("Mock charge of {} for booking {} -> {}", amount, bookingId, transactionId);
        return transactionId;
    }

    @Override
    public void refund(String transactionId, BigDecimal amount) {
        log.info("Mock refund of {} for transaction {}", amount, transactionId);
    }
}
