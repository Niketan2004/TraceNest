package com.tracenest.backend.Subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
}
