package com.tracenest.backend.User.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String phone;
    private String city;
    private String avatarUrl;
}
