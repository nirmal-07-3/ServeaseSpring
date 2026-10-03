package com.application.Servease.booking.entity;

import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_service_id", nullable = false)
    private ProviderService providerService;


    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime bookingTime;


    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedPrice;


    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 20)
    private String pincode;


    @Column(length = 1000)
    private String customerNote;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}