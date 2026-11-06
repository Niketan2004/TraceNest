package com.tracenest.backend.Subscription.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;

/**
 * JPA entity representing a public community alert subscriber verified via SMS OTP.
 */
@Entity
@Table(name = "subscribers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscriber {
    
    @Id
    @UuidGenerator
    private String id;
    
    private String fullName;
    private String email;
    private String mobileNumber;
    
    private Double lat;
    private Double lng;
    private String address;
    private String city;

    @Builder.Default
    private Boolean isVerified = false;

    private OffsetDateTime verifiedAt;

    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
