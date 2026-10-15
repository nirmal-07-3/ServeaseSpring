package com.application.Servease.favorite.entity;

import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_providers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_favorite_customer_provider",
                        columnNames = {"customer_id", "provider_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FavoriteProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
