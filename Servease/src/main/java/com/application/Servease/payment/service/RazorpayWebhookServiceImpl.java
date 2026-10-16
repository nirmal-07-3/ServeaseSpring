package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.entity.RazorpayWebhookEvent;
import com.application.Servease.payment.enums.CollectionMethod;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.BookingCollectionRepository;
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
    private final BookingCollectionRepository collectionRepository;
    private final JsonMapper jsonMapper;
    private final RazorpayWebhookEventRepository webhookEventRepository;
    private final BookingRepository bookingRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Override
    @Transactional
    public void processWebhook(String payload, String signature, String eventId) {
        if (payload == null || payload.isBlank()
                || signature == null || signature.isBlank()
                || eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Required webhook data is missing.");
        }

        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature.");
        }

        if (webhookEventRepository.existsByEventId(eventId)) {
            return;
        }

        try {
            JsonNode root = jsonMapper.readTree(payload);
            String eventType = root.path("event").asText("");

            if (eventType.isBlank()) {
                throw new IllegalArgumentException("Webhook event type is missing.");
            }

            RazorpayWebhookEvent event = RazorpayWebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .receivedAt(LocalDateTime.now())
                    .processingStatus("PROCESSING")
                    .attemptCount(1)
                    .build();

            webhookEventRepository.saveAndFlush(event);

            if ("payment.captured".equals(eventType)) {
                JsonNode paymentEntity = root.path("payload")
                        .path("payment")
                        .path("entity");

                String qrCodeId = paymentEntity.path("qr_code_id").asText("");

                if (!qrCodeId.isBlank()) {
                    processCapturedQrPayment(paymentEntity, qrCodeId);
                } else {
                    processCapturedOrderPayment(paymentEntity);
                }
            }

            event.setProcessingStatus("PROCESSED");
            event.setProcessedAt(LocalDateTime.now());
            event.setNextRetryAt(null);
            event.setLastError(null);

            webhookEventRepository.saveAndFlush(event);

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to process Razorpay webhook. The gateway may retry this event.",
                    ex
            );
        }
    }

    private void processCapturedOrderPayment(JsonNode paymentEntity) {
        String paymentId = paymentEntity.path("id").asText("");
        String orderId = paymentEntity.path("order_id").asText("");
        String currency = paymentEntity.path("currency").asText("");
        String status = paymentEntity.path("status").asText("");
        boolean captured = paymentEntity.path("captured").asBoolean(false);
        long amountPaise = paymentEntity.path("amount").asLong(-1);

        if (paymentId.isBlank() || orderId.isBlank() || amountPaise <= 0
                || !"INR".equalsIgnoreCase(currency)
                || (!captured && !"captured".equalsIgnoreCase(status))) {
            throw new IllegalArgumentException(
                    "Captured payment details are incomplete or invalid."
            );
        }

        Payment payment = paymentRepository.findByGatewayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No local payment matches this Razorpay order. Reconciliation is required."
                ));

        PaymentAttempt attempt = paymentAttemptRepository
                .findByGatewayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment attempt matches this Razorpay order. Reconciliation is required."
                ));

        if (!payment.getBooking().getId().equals(attempt.getBooking().getId())) {
            throw new IllegalArgumentException(
                    "The payment and attempt belong to different bookings."
            );
        }

        if (payment.getAmount() == null || attempt.getAmount() == null
                || payment.getAmount().compareTo(attempt.getAmount()) != 0
                || !"INR".equalsIgnoreCase(attempt.getCurrency())) {
            throw new IllegalArgumentException(
                    "The local payment amount or currency is inconsistent."
            );
        }

        long expectedPaise = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();

        if (!"captured".equalsIgnoreCase(status)
                || !captured
                || expectedPaise != amountPaise) {
            throw new IllegalArgumentException(
                    "Razorpay captured payment does not match the local amount."
            );
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS
                && !paymentId.equals(payment.getTransactionId())) {
            throw new IllegalArgumentException(
                    "A different transaction is already recorded for this payment."
            );
        }

        if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED
                && !paymentId.equals(attempt.getGatewayPaymentId())) {
            throw new IllegalArgumentException(
                    "A different gateway payment is already recorded for this attempt."
            );
        }

        if (payment.getTransactionId() != null
                && !payment.getTransactionId().isBlank()
                && !paymentId.equals(payment.getTransactionId())) {
            throw new IllegalArgumentException(
                    "The local payment already references another transaction."
            );
        }

        if (attempt.getGatewayPaymentId() != null
                && !attempt.getGatewayPaymentId().isBlank()
                && !paymentId.equals(attempt.getGatewayPaymentId())) {
            throw new IllegalArgumentException(
                    "The attempt already references another gateway payment."
            );
        }

        payment.setTransactionId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);

        attempt.setGatewayPaymentId(paymentId);
        attempt.setStatus(PaymentAttemptStatus.CAPTURED);

        if (attempt.getCapturedAt() == null) {
            attempt.setCapturedAt(LocalDateTime.now());
        }

        paymentAttemptRepository.saveAndFlush(attempt);
        paymentRepository.saveAndFlush(payment);
    }

    private void processCapturedQrPayment(JsonNode paymentEntity, String qrCodeId) {
        String paymentId = paymentEntity.path("id").asText("");
        String currency = paymentEntity.path("currency").asText("");
        String status = paymentEntity.path("status").asText("");
        boolean captured = paymentEntity.path("captured").asBoolean(false);
        long amountPaise = paymentEntity.path("amount").asLong(-1);

        if (paymentId.isBlank() || amountPaise <= 0
                || !"INR".equalsIgnoreCase(currency)
                || !captured
                || !"captured".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                    "Captured QR payment details are incomplete or invalid."
            );
        }

        BookingCollection initialCollection = collectionRepository
                .findByGatewayQrCodeId(qrCodeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No booking collection matches this QR code. Reconciliation is required."
                ));

        Booking initialBooking = initialCollection.getBooking();

        Booking booking = bookingRepository
                .findByIdAndProviderServiceProviderForUpdate(
                        initialBooking.getId(),
                        initialBooking.getProviderService().getProvider()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "The booking associated with this QR code was not found."
                ));

        BookingCollection collection = collectionRepository
                .findByGatewayQrCodeIdForUpdate(qrCodeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No booking collection matches this QR code. Reconciliation is required."
                ));

        if (!booking.getId().equals(collection.getBooking().getId())) {
            throw new IllegalArgumentException(
                    "The QR collection does not belong to the locked booking."
            );
        }

        if (collection.getStatus() == CollectionStatus.CONFIRMED) {
            if (paymentId.equals(collection.getGatewayPaymentId())) {
                if (booking.getStatus() != BookingStatus.COMPLETED) {
                    throw new IllegalArgumentException(
                            "The collection is confirmed, but the booking is not completed. Reconciliation is required."
                    );
                }
                return;
            }

            throw new IllegalArgumentException(
                    "A different payment already confirmed this collection."
            );
        }

        if (collection.getStatus() != CollectionStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This collection is not awaiting payment."
            );
        }

        String effectivePreference = booking.getEffectivePaymentPreference();

        if (effectivePreference == null || effectivePreference.isBlank()) {
            PaymentPreference preference = booking.getPaymentPreference();
            effectivePreference = preference == null ? null : preference.name();
        }

        if (booking.getStatus() != BookingStatus.IN_PROGRESS
                || !PaymentPreference.PAY_AT_SERVICE.name().equals(effectivePreference)) {
            throw new IllegalArgumentException(
                    "The booking is not eligible for QR collection completion."
            );
        }

        if (collection.getAmount() == null || collection.getAmount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "The booking collection amount is invalid."
            );
        }

        long expectedPaise = collection.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();

        if (expectedPaise != amountPaise
                || !"INR".equalsIgnoreCase(collection.getCurrency())) {
            throw new IllegalArgumentException(
                    "QR payment amount or currency does not match the booking collection."
            );
        }

        PaymentAttempt attempt = collection.getPaymentAttempt();

        if (attempt == null
                || attempt.getBooking() == null
                || !attempt.getBooking().getId().equals(collection.getBooking().getId())) {
            throw new IllegalArgumentException(
                    "The collection does not have a valid payment attempt."
            );
        }

        if (attempt.getAmount() == null
                || attempt.getAmount().compareTo(collection.getAmount()) != 0
                || !"INR".equalsIgnoreCase(attempt.getCurrency())) {
            throw new IllegalArgumentException(
                    "The QR payment attempt amount or currency is inconsistent."
            );
        }

        if (attempt.getGatewayPaymentId() != null
                && !attempt.getGatewayPaymentId().isBlank()
                && !paymentId.equals(attempt.getGatewayPaymentId())) {
            throw new IllegalArgumentException(
                    "The attempt already references a different gateway payment."
            );
        }

        if (paymentAttemptRepository.findByGatewayPaymentId(paymentId)
                .filter(existing -> !existing.getId().equals(attempt.getId()))
                .isPresent()) {
            throw new IllegalArgumentException(
                    "This Razorpay payment ID is already assigned to another attempt."
            );
        }

        if (collectionRepository.findByGatewayPaymentId(paymentId)
                .filter(existing -> !existing.getId().equals(collection.getId()))
                .isPresent()) {
            throw new IllegalArgumentException(
                    "This Razorpay payment ID is already assigned to another collection."
            );
        }

        LocalDateTime confirmedAt = LocalDateTime.now();

        attempt.setGatewayPaymentId(paymentId);
        attempt.setStatus(PaymentAttemptStatus.CAPTURED);

        if (attempt.getCapturedAt() == null) {
            attempt.setCapturedAt(confirmedAt);
        }

        collection.setCollectionMethod(CollectionMethod.ONLINE);
        collection.setGatewayPaymentId(paymentId);
        collection.setStatus(CollectionStatus.CONFIRMED);
        collection.setConfirmedAt(confirmedAt);
        collection.setConfirmedBy(null);

        booking.setStatus(BookingStatus.COMPLETED);

        paymentAttemptRepository.saveAndFlush(attempt);
        collectionRepository.saveAndFlush(collection);
        bookingRepository.saveAndFlush(booking);
    }
}
