package com.application.Servease.admin.service;

import com.application.Servease.admin.dto.AdminAnalyticsResponse;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.BookingCollectionRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.user.enums.UserRole;
import com.application.Servease.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final PaymentRepository paymentRepository;
    private final BookingCollectionRepository bookingCollectionRepository;

    public AdminAnalyticsResponse getOverview() {
        BigDecimal onlineFees =
                paymentRepository.sumPlatformFeesByStatus(
                        PaymentStatus.SUCCESS
                );

        BigDecimal collectionFees =
                bookingCollectionRepository.sumPlatformFeesByStatus(
                        CollectionStatus.CONFIRMED
                );

        BigDecimal platformRevenue =
                onlineFees.add(collectionFees);

        return new AdminAnalyticsResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.PROVIDER),
                bookingRepository.count(),
                platformRevenue,
                providerProfileRepository.countByVerificationStatus(
                        VerificationStatus.PENDING
                )
        );
    }
}