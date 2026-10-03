package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.common.exception.PaymentNotFoundException;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.dto.RazorpayOrderResponse;
import com.application.Servease.payment.dto.VerifyPaymentRequest;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.mapper.PaymentMapper;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.payment.service.RazorpayPaymentService;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class RazorpayPaymentServiceImpl
        implements RazorpayPaymentService {

    private final RazorpayClient razorpayClient;

    private final BookingRepository bookingRepository;

    private final PaymentRepository paymentRepository;

    private final UserService userService;

    private final PaymentMapper paymentMapper;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    public RazorpayOrderResponse createOrder(
            Long bookingId) {

        // 1. Get authenticated customer
        User customer =
                userService.getAuthenticatedUser();

        // 2. Find customer's booking
        Booking booking =
                bookingRepository
                        .findByIdAndCustomer(
                                bookingId,
                                customer
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        // 3. Booking must be accepted
        if (booking.getStatus()
                != BookingStatus.ACCEPTED) {

            throw new PaymentConflictException(
                    "Payment can only be created for an accepted booking."
            );
        }

        // 4. Check existing payment
        Payment payment =
                paymentRepository
                        .findByBookingId(bookingId)
                        .orElseThrow(() ->
                                new PaymentConflictException(
                                        "Payment record not found. Create the payment first."
                                ));

        // 5. Don't create another Razorpay order
        if (payment.getGatewayOrderId() != null) {

            throw new PaymentConflictException(
                    "Razorpay order already exists for this payment."
            );
        }

        // 6. Get amount from Booking
        BigDecimal amount =
                booking.getAgreedPrice();

        // Razorpay expects amount in paise
        long amountInPaise =
                amount
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact();

        // 7. Prepare Razorpay order request
        JSONObject orderRequest =
                new JSONObject();

        orderRequest.put(
                "amount",
                amountInPaise
        );

        orderRequest.put(
                "currency",
                "INR"
        );

        orderRequest.put(
                "receipt",
                "SERVEASE_" + booking.getId()
        );

        // 8. Create Razorpay order
        Order razorpayOrder;

        try {

            razorpayOrder =
                    razorpayClient.orders.create(
                            orderRequest
                    );

        } catch (Exception ex) {

            throw new PaymentConflictException(
                    "Unable to create Razorpay order."
            );
        }



        // 9. Save Razorpay order ID
        String razorpayOrderId =
                razorpayOrder.get("id");

        payment.setGatewayOrderId(
                razorpayOrderId
        );

        payment.setStatus(
                PaymentStatus.PENDING
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        // 10. Return order details
        return RazorpayOrderResponse.builder()
                .paymentId(savedPayment.getId())
                .bookingId(booking.getId())
                .razorpayOrderId(
                        razorpayOrderId
                )
                .razorpayKeyId(
                        razorpayKeyId
                )
                .amount(amount)
                .currency("INR")
                .status("created")
                .build();
    }


    @Override
    public PaymentResponse verifyPayment(
            VerifyPaymentRequest request) {

        // 1. Find our payment using Razorpay Order ID
        Payment payment =
                paymentRepository
                        .findByGatewayOrderId(
                                request.getRazorpayOrderId()
                        )
                        .orElseThrow(() ->
                                new PaymentNotFoundException(
                                        "Payment not found for this Razorpay order."
                                ));

        // 2. Get authenticated customer
        User customer =
                userService.getAuthenticatedUser();

        // 3. Make sure this payment belongs to
        //    the authenticated customer's booking
        if (!payment.getBooking()
                .getCustomer()
                .getId()
                .equals(customer.getId())) {

            throw new PaymentConflictException(
                    "You are not authorized to verify this payment."
            );
        }

        // 4. Prevent re-processing an already successful payment
        if (payment.getStatus()
                == PaymentStatus.SUCCESS) {

            return paymentMapper.toResponse(payment);
        }

        /*
         * 5. Verify Razorpay signature.
         *
         * Razorpay signature verification uses:
         *
         * order_id + "|" + payment_id
         *
         * together with your Razorpay Key Secret.
         */
        try {

            String payload =
                    request.getRazorpayOrderId()
                            + "|"
                            + request.getRazorpayPaymentId();

            Utils.verifySignature(
                    payload,
                    request.getRazorpaySignature(),
                    razorpayKeySecret
            );

        } catch (Exception ex) {

            throw new PaymentConflictException(
                    "Razorpay payment signature verification failed."
            );
        }

        // 6. Signature is valid
        payment.setTransactionId(
                request.getRazorpayPaymentId()
        );

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        // 7. Save updated payment
        Payment savedPayment =
                paymentRepository.save(payment);

        return paymentMapper.toResponse(
                savedPayment
        );
    }
}