package com.tracenest.backend.Subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private String id;
    private String city;
    private List<String> types;
    private Double radiusKm;
    private Double lat;
    private Double lng;
    private OffsetDateTime createdAt;
}
