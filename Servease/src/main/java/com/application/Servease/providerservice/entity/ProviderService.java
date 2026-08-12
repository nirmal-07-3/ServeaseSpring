package com.application.Servease.providerservice.entity;

import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.subcategory.entity.SubCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "provider_services",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "provider_id",
                                "sub_category_id",
                                "title"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Provider who offers this service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    /*
     * Type/category of service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    /*
     * Provider's service title.
     */
    @Column(nullable = false)
    private String title;

    /*
     * Details about what the service includes.
     */
    @Column(length = 2000)
    private String description;

    /*
     * Starting/base price of the service.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    /*
     * Estimated service duration in minutes.
     */
    @Column(nullable = false)
    private Integer estimatedDurationMinutes;

    /*
     * Maximum distance provider is willing to travel.
     */
    @Column(nullable = false)
    private Double serviceRadiusKm;

    /*
     * Provider can enable/disable the service.
     */
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}