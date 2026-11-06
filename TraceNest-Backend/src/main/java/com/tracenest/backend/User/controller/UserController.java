package com.tracenest.backend.User.controller;

import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Auth.service.AuthService;
import com.tracenest.backend.Common.dto.MessageResponse;
import com.tracenest.backend.User.dto.ChangePasswordRequest;
import com.tracenest.backend.User.dto.UpdateUserRequest;
import com.tracenest.backend.User.dto.UserDto;
import com.tracenest.backend.User.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller managing user profiles, Cloudinary avatar uploads,
 * authenticated password modifications, and account terminations.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Retrieves the profile data of the currently logged-in user.
     *
     * @param userDetails Authenticated user security principal.
     * @return {@link ResponseEntity} containing {@link UserDto}.
     */
    @GetMapping
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getId()));
    }

    /**
     * Partially updates editable profile attributes (name, phone, city) of the user.
     *
     * @param userDetails Authenticated user principal.
     * @param request Payload containing optional profile field updates.
     * @return {@link ResponseEntity} with updated {@link UserDto}.
     */
    @PatchMapping
    public ResponseEntity<UserDto> updateUser(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                              @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userDetails.getId(), request));
    }

    /**
     * Uploads and stores a new profile avatar image via Cloudinary CDN.
     *
     * @param userDetails Authenticated user principal.
     * @param file Multipart avatar image file.
     * @return Map containing the public CDN avatarUrl.
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @RequestParam("file") MultipartFile file) {
        String url = userService.uploadAvatar(userDetails.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    /**
     * Changes the user password after verifying the current password against BCrypt hash.
     *
     * @param userDetails Authenticated user principal.
     * @param request Validated password change request containing old and new passwords.
     * @return Confirmation message response.
     */
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(new MessageResponse("Password updated."));
    }

    /**
     * Permanently deletes the user account, cascading deletion across reports and alerts.
     *
     * @param userDetails Authenticated user principal.
     * @return Confirmation message response.
     */
    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteAccount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.deleteUser(userDetails.getId());
        return ResponseEntity.ok(new MessageResponse("Account deleted successfully."));
    }
}
