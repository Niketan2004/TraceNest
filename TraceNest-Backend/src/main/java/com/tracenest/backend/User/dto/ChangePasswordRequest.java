package com.tracenest.backend.User.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;
    
    @NotBlank
    private String newPassword;
}
