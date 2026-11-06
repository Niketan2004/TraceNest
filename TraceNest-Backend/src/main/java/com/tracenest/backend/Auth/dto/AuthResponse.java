package com.tracenest.backend.Auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tracenest.backend.User.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private UserDto user;
}
