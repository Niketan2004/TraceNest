package com.tracenest.backend.User.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UserDto {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String role;
    private String avatarUrl;
    private OffsetDateTime createdAt;

    public UserDto() {
    }

    public UserDto(String id, String name, String email, String phone, String city, String role, String avatarUrl, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
    }
}
