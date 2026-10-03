package com.application.Servease.payment.service;

import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.RazorpayWebhookEvent;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.payment.repository.RazorpayWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.razorpay.Utils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookServiceImpl
        implements RazorpayWebhookService {





    private final PaymentRepository paymentRepository;

    private final JsonMapper jsonMapper;

    private final RazorpayWebhookEventRepository webhookEventRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;


    @Override
    @Transactional
    public void processWebhook(
            String payload,
            String signature,
            String eventId
    ) {

        // 1. Verify Razorpay signature

        try {

            Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Invalid Razorpay webhook signature."
            );
        }


        // 2. Check duplicate event

        if (webhookEventRepository.existsByEventId(eventId)) {
            return;
        }


        try {

            JsonNode root =
                    jsonMapper.readTree(payload);

            String event =
                    root.path("event")
                            .asText();


            // 3. Store event ID BEFORE processing

            RazorpayWebhookEvent webhookEvent =
                    RazorpayWebhookEvent.builder()
                            .eventId(eventId)
                            .eventType(event)
                            .receivedAt(LocalDateTime.now())
                            .build();

            System.out.println("Saving webhook event: " + eventId);

            webhookEventRepository.save(webhookEvent);


            // 4. Handle payment.captured

            if ("payment.captured".equals(event)) {

                JsonNode paymentEntity =
                        root.path("payload")
                                .path("payment")
                                .path("entity");

                String razorpayPaymentId =
                        paymentEntity
                                .path("id")
                                .asText();

                String razorpayOrderId =
                        paymentEntity
                                .path("order_id")
                                .asText();


                if (razorpayOrderId.isBlank()) {

                    throw new IllegalArgumentException(
                            "Razorpay order ID missing from webhook."
                    );
                }


                // 5. Find our payment

                Payment payment =
                        paymentRepository
                                .findByGatewayOrderId(
                                        razorpayOrderId
                                )
                                .orElse(null);


                if (payment == null) {
                    return;
                }


                // 6. Idempotency at payment level

                if (payment.getStatus()
                        == PaymentStatus.SUCCESS) {

                    return;
                }


                // 7. Update payment

                payment.setTransactionId(
                        razorpayPaymentId
                );

                payment.setStatus(
                        PaymentStatus.SUCCESS
                );

                paymentRepository.save(payment);
            }

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Unable to process Razorpay webhook.",
                    e
            );
        }
    }
}