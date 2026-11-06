package com.tracenest.backend.Auth.service;

import com.tracenest.backend.Auth.dto.*;
import com.tracenest.backend.Auth.security.JwtUtils;
import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.User.dto.UserDto;
import com.tracenest.backend.User.mapper.UserMapper;
import com.tracenest.backend.User.model.Role;
import com.tracenest.backend.User.model.User;
import com.tracenest.backend.User.repository.UserRepository;
import com.tracenest.backend.Notification.service.NotificationDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service encapsulating business logic for user authentication, registration,
 * JWT lifecycle management, session revocation, and password recovery.
 */
@Service
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final NotificationDeliveryService deliveryService;
    private final com.tracenest.backend.Notification.service.NotificationService notificationService;

    /**
     * Constructs the authentication service with required dependencies.
     */
    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder encoder,
            JwtUtils jwtUtils,
            UserMapper userMapper,
            NotificationDeliveryService deliveryService,
            com.tracenest.backend.Notification.service.NotificationService notificationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.deliveryService = deliveryService;
        this.notificationService = notificationService;
    }

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenDurationMs;

    /**
     * Registers a new user, hashes the password via BCrypt, dispatches welcome notifications,
     * and signs an initial authentication token pair.
     *
     * @param signUpRequest Validated registration form payload.
     * @return Generated {@link AuthResponse} containing access token and user metadata.
     * @throws CustomException if the email is already registered in the system.
     */
    public AuthResponse registerUser(RegisterRequest signUpRequest) {
        String cleanEmail = signUpRequest.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "Conflict", "EMAIL_TAKEN", "Error: Email is already in use!");
        }

        User user = User.builder()
                .name(signUpRequest.getName().trim())
                .email(cleanEmail)
                .password(encoder.encode(signUpRequest.getPassword()))
                .phone(signUpRequest.getPhone())
                .city(signUpRequest.getCity())
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        // Send welcome email to newly registered user
        try {
            String welcomeSubject = "Welcome to TraceNest – Protecting Our Communities Together";
            String welcomeBody = String.format(
                    "Hi %s,\n\nWelcome to TraceNest!\n\nYour account has been created successfully. With TraceNest, you can:\n" +
                    "• File and browse community reports for missing persons, lost pets, and stolen vehicles.\n" +
                    "• Generate printable search flyers and emergency posters instantly.\n" +
                    "• Set up geo-targeted alerts for your neighborhood or city.\n" +
                    "• Receive real-time community updates.\n\n" +
                    "Together, we can bring our loved ones home.\n\n" +
                    "— The TraceNest Team\nhttps://tracenest.app",
                    user.getName()
            );
            deliveryService.sendEmail(cleanEmail, welcomeSubject, welcomeBody);
            log.info("Dispatched welcome email to registered user: {}", cleanEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", cleanEmail, e);
        }

        // Create welcome in-app notification
        try {
            notificationService.createNotification(
                    user,
                    "WELCOME",
                    "Welcome to TraceNest!",
                    "Thank you for joining our community-based lost & found platform. You can report incidents, configure local alerts, and help reunite families.",
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to create welcome in-app notification for user {}: {}", user.getId(), e.getMessage());
        }

        return authenticateAndGenerateTokens(cleanEmail, signUpRequest.getPassword());
    }

    /**
     * Authenticates existing user credentials against Spring Security's authentication manager.
     *
     * @param loginRequest Request containing email and plaintext password.
     * @return Generated {@link AuthResponse} containing new session tokens.
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        return authenticateAndGenerateTokens(loginRequest.getEmail().trim().toLowerCase(), loginRequest.getPassword());
    }

    /**
     * Helper to authenticate credentials, populate SecurityContext, and persist a fresh refresh token.
     */
    private AuthResponse authenticateAndGenerateTokens(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "User not found"));

        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(OffsetDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpirationMs / 1000)
                .user(userMapper.userToUserDto(user))
                .build();
    }

    /**
     * Validates an unexpired refresh token and mints a new JWT access token without re-entering password.
     *
     * @param request Payload containing the refresh token UUID.
     * @return New {@link AuthResponse} with fresh access token.
     * @throws CustomException if the refresh token is unknown or expired.
     */
    public AuthResponse refreshToken(RefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        
        User user = userRepository.findByRefreshToken(requestRefreshToken)
                .orElseThrow(() -> new CustomException(401, "Unauthorized", "TOKEN_INVALID", "Refresh token is not in database!"));

        if (user.getRefreshTokenExpiry().compareTo(OffsetDateTime.now()) < 0) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            throw new CustomException(401, "Unauthorized", "TOKEN_EXPIRED", "Refresh token was expired. Please make a new signin request");
        }

        String token = jwtUtils.generateTokenFromUserId(user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(requestRefreshToken)
                .expiresIn(jwtExpirationMs / 1000)
                .user(userMapper.userToUserDto(user))
                .build();
    }

    /**
     * Invalidates active session by removing the persisted refresh token.
     *
     * @param userId The database primary key of the target user.
     */
    public void logout(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Generates a 1-hour password reset token and sends an asynchronous recovery email.
     *
     * @param email The account email address.
     */
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) return;
        String cleanEmail = email.trim().toLowerCase();
        
        userRepository.findByEmail(cleanEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(OffsetDateTime.now().plusHours(1));
            userRepository.save(user);
            
            // Send reset email asynchronously
            String resetLink = "http://localhost:5173/reset-password?token=" + token;
            String body = String.format(
                "Hi %s,\n\nYou requested a password reset for your TraceNest account.\n\n" +
                "Click the link below to reset your password:\n%s\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request this, please ignore this email.\n\n— TraceNest Team",
                user.getName(), resetLink
            );
            deliveryService.sendEmail(user.getEmail(), "TraceNest — Password Reset", body);
            log.info("Password reset email queued for user {}", user.getId());
        });
    }

    /**
     * Validates a password reset token and updates the user's encoded password.
     *
     * @param token Reset token string.
     * @param newPassword New plaintext password meeting complexity criteria.
     * @throws CustomException if the token is invalid, expired, or password is insufficient.
     */
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new CustomException(400, "Bad Request", "INVALID_TOKEN", "Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new CustomException(400, "Bad Request", "WEAK_PASSWORD", "Password must be at least 8 characters");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new CustomException(400, "Bad Request", "INVALID_TOKEN", "Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(OffsetDateTime.now())) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new CustomException(400, "Bad Request", "TOKEN_EXPIRED", "Reset token has expired");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successfully for user {}", user.getId());
    }

    /**
     * Resolves the user entity by ID and transforms it to an API-safe {@link UserDto}.
     *
     * @param userId Primary key of the user.
     * @return Transformed {@link UserDto}.
     * @throws CustomException if no user is found with the given ID.
     */
    public UserDto getCurrentUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "User not found"));
        return userMapper.userToUserDto(user);
    }
}
