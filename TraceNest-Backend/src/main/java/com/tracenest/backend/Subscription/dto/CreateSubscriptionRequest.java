package com.tracenest.backend.Subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {
    @NotBlank(message = "City is required")
    private String city;
    private List<String> types;
    private Double radiusKm;
    private Double lat;
    private Double lng;
}
