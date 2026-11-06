package com.tracenest.backend.Auth.controller;

import com.tracenest.backend.Auth.dto.*;
import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Auth.service.AuthService;
import com.tracenest.backend.Common.dto.MessageResponse;
import com.tracenest.backend.User.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller managing authentication, registration, session renewal, and password recovery.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>User registration with BCrypt hashed credentials</li>
 *   <li>Stateless JWT login issuing access and refresh tokens</li>
 *   <li>Silent token refresh via valid refresh tokens</li>
 *   <li>User logout and token revocation</li>
 *   <li>Password recovery via secure email reset tokens</li>
 *   <li>Identity inspection for the currently authenticated principal</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account with validated credentials and assigns the default ROLE_USER.
     *
     * @param signUpRequest Validated payload containing name, email, password, phone, and city.
     * @return {@link ResponseEntity} with HTTP 201 Created and the issued {@link AuthResponse}.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        AuthResponse response = authService.registerUser(signUpRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Authenticates a user against database credentials and returns a JWT access & refresh token pair.
     *
     * @param loginRequest Validated payload containing user email and plaintext password.
     * @return {@link ResponseEntity} with HTTP 200 OK and {@link AuthResponse}.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Exchanges a valid refresh token for a newly generated JWT access token.
     *
     * @param request Validated payload containing the unexpired refresh token.
     * @return {@link ResponseEntity} with HTTP 200 OK and updated {@link AuthResponse}.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * Invalidates the active session and clears the refresh token for the authenticated user.
     *
     * @param userDetails The currently authenticated security principal.
     * @return {@link ResponseEntity} with HTTP 204 No Content.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails != null) {
            authService.logout(userDetails.getId());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Initiates a password reset workflow by generating a time-limited token and dispatching an email.
     *
     * @param body JSON map containing the target account email.
     * @return {@link ResponseEntity} with HTTP 202 Accepted and confirmation message.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        authService.forgotPassword(body.get("email"));
        return ResponseEntity.accepted().body(new MessageResponse("If the email exists, a reset link was sent."));
    }

    /**
     * Consumes a valid password reset token and sets the new BCrypt hashed password.
     *
     * @param body JSON map containing the verification token and new password string.
     * @return {@link ResponseEntity} with HTTP 200 OK and status message.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody java.util.Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(new MessageResponse("Password updated."));
    }

    /**
     * Retrieves the profile and authorization details of the currently authenticated principal.
     *
     * @param userDetails The resolved security principal from the Authorization header.
     * @return {@link ResponseEntity} containing {@link UserDto} or HTTP 401 Unauthorized.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Full authentication is required to access this resource"));
        }
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getId()));
    }
}
