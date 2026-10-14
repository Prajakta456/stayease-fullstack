package com.stayease.service.payment;

import java.math.BigDecimal;

/**
 * Charges and refunds guests. The app ships with {@link MockPaymentGateway}; a real
 * provider (Stripe, Razorpay, ...) can be plugged in by implementing this interface.
 */
public interface PaymentGateway {

    /**
     * @return the provider's transaction id
     * @throws PaymentFailedException if the charge is declined
     */
    String charge(Long bookingId, BigDecimal amount);

    void refund(String transactionId, BigDecimal amount);
}
