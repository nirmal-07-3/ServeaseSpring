
package com.application.Servease.payment.service;

import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.entity.RazorpayWebhookEvent;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.payment.repository.RazorpayWebhookEventRepository;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookServiceImpl implements RazorpayWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final JsonMapper jsonMapper;
    private final RazorpayWebhookEventRepository webhookEventRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Override
    @Transactional
    public void processWebhook(
            String payload,
            String signature,
            String eventId) {

        if (payload == null || payload.isBlank()
                || signature == null || signature.isBlank()
                || eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Required webhook data is missing.");
        }

        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Invalid Razorpay webhook signature.");
        }

        if (webhookEventRepository.existsByEventId(eventId)) {
            return;
        }

        try {
            JsonNode root = jsonMapper.readTree(payload);
            String event = root.path("event").asText("");

            if (event.isBlank()) {
                throw new IllegalArgumentException(
                        "Webhook event type is missing.");
            }

            if ("payment.captured".equals(event)) {
                processCapturedPayment(root);
            }

            RazorpayWebhookEvent webhookEvent =
                    RazorpayWebhookEvent.builder()
                            .eventId(eventId)
                            .eventType(event)
                            .receivedAt(LocalDateTime.now())
                            .build();

            webhookEventRepository.saveAndFlush(webhookEvent);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to process Razorpay webhook.", ex);
        }
    }

    private void processCapturedPayment(JsonNode root) {
        JsonNode paymentEntity = root.path("payload")
                .path("payment")
                .path("entity");

        String razorpayPaymentId = paymentEntity.path("id").asText("");
        String razorpayOrderId = paymentEntity.path("order_id").asText("");
        String currency = paymentEntity.path("currency").asText("");
        String gatewayStatus = paymentEntity.path("status").asText("");
        boolean captured = paymentEntity.path("captured").asBoolean(false);
        long amountInPaise = paymentEntity.path("amount").asLong(-1);

        if (razorpayPaymentId.isBlank()
                || razorpayOrderId.isBlank()
                || amountInPaise <= 0
                || !"INR".equalsIgnoreCase(currency)
                || (!captured && !"captured".equalsIgnoreCase(gatewayStatus))) {
            throw new IllegalArgumentException(
                    "Captured payment details are incomplete or invalid.");
        }

        Payment payment = paymentRepository
                .findByGatewayOrderIdForUpdate(razorpayOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No local payment matches this Razorpay order. "
                                        + "The event requires reconciliation."));

        PaymentAttempt attempt = paymentAttemptRepository
                .findByGatewayOrderId(razorpayOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No payment attempt matches this Razorpay order. "
                                        + "The event requires reconciliation."));

        if (!payment.getBooking().getId().equals(attempt.getBooking().getId())) {
            throw new IllegalArgumentException(
                    "The payment and payment attempt belong to different bookings.");
        }

        if (payment.getAmount() == null
                || attempt.getAmount() == null
                || payment.getAmount().compareTo(attempt.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    "The legacy payment and payment attempt amounts do not match.");
        }

        BigDecimal expectedPaise = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY);

        if (expectedPaise.compareTo(BigDecimal.valueOf(amountInPaise)) != 0) {
            throw new IllegalArgumentException(
                    "Razorpay amount does not match the local payment.");
        }

        if (!"INR".equalsIgnoreCase(attempt.getCurrency())) {
            throw new IllegalArgumentException(
                    "The payment attempt currency is invalid.");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS
                && !razorpayPaymentId.equals(payment.getTransactionId())) {
            throw new IllegalArgumentException(
                    "A different transaction is already recorded for this payment.");
        }

        if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED
                && !razorpayPaymentId.equals(attempt.getGatewayPaymentId())) {
            throw new IllegalArgumentException(
                    "A different gateway payment is already recorded for this attempt.");
        }

        if (payment.getTransactionId() != null
                && !payment.getTransactionId().isBlank()
                && !razorpayPaymentId.equals(payment.getTransactionId())) {
            throw new IllegalArgumentException(
                    "The local payment already references another transaction.");
        }

        if (attempt.getGatewayPaymentId() != null
                && !attempt.getGatewayPaymentId().isBlank()
                && !razorpayPaymentId.equals(attempt.getGatewayPaymentId())) {
            throw new IllegalArgumentException(
                    "The payment attempt already references another gateway payment.");
        }

        payment.setTransactionId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.SUCCESS);

        attempt.setGatewayPaymentId(razorpayPaymentId);
        attempt.setStatus(PaymentAttemptStatus.CAPTURED);

        if (attempt.getCapturedAt() == null) {
            attempt.setCapturedAt(LocalDateTime.now());
        }

        paymentAttemptRepository.saveAndFlush(attempt);
        paymentRepository.saveAndFlush(payment);
    }
}
