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

    /*
     * Customer who created the booking.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /*
     * Service selected by the customer.
     * Provider is derived through ProviderService.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_service_id", nullable = false)
    private ProviderService providerService;

    /*
     * Requested service date.
     */
    @Column(nullable = false)
    private LocalDate bookingDate;

    /*
     * Requested service start time.
     */
    @Column(nullable = false)
    private LocalTime bookingTime;

    /*
     * Price agreed at the time of booking.
     *
     * This is intentionally separate from
     * ProviderService.basePrice because the
     * provider may change the service price later.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedPrice;

    /*
     * Address where the service will be performed.
     */
    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 20)
    private String pincode;

    /*
     * Additional information provided by the customer.
     */
    @Column(length = 1000)
    private String customerNote;

    /*
     * Current booking status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}