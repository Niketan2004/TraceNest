package com.tracenest.backend.Auth.service;

import com.tracenest.backend.Auth.dto.AuthResponse;
import com.tracenest.backend.Auth.dto.LoginRequest;
import com.tracenest.backend.Auth.dto.RegisterRequest;
import com.tracenest.backend.Auth.security.JwtUtils;
import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Notification.service.NotificationDeliveryService;
import com.tracenest.backend.User.mapper.UserMapper;
import com.tracenest.backend.User.model.Role;
import com.tracenest.backend.User.model.User;
import com.tracenest.backend.User.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NotificationDeliveryService deliveryService;
    @Mock
    private com.tracenest.backend.Notification.service.NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 900000);
        ReflectionTestUtils.setField(authService, "refreshTokenDurationMs", 2592000000L);
    }

    @Test
    void testRegisterUser_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("test@tracenest.com");
        req.setPassword("SecureP@ss123");

        when(userRepository.existsByEmail("test@tracenest.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pw");

        User savedUser = User.builder()
                .id("user-1")
                .name("Test User")
                .email("test@tracenest.com")
                .role(Role.USER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(savedUser));

        com.tracenest.backend.Auth.security.UserDetailsImpl principal =
                com.tracenest.backend.Auth.security.UserDetailsImpl.build(savedUser);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("mock_access_token");

        AuthResponse res = authService.registerUser(req);

        assertNotNull(res);
        assertEquals("mock_access_token", res.getAccessToken());
        assertNotNull(res.getRefreshToken());
        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(deliveryService).sendEmail(eq("test@tracenest.com"), contains("Welcome"), anyString());
        verify(notificationService).createNotification(eq(savedUser), eq("WELCOME"), anyString(), anyString(), isNull());
    }

    @Test
    void testRegisterUser_DuplicateEmailThrows() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("existing@tracenest.com");
        req.setPassword("SecureP@ss123");

        when(userRepository.existsByEmail("existing@tracenest.com")).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> authService.registerUser(req));
        assertEquals("EMAIL_TAKEN", ex.getCode());
    }

    @Test
    void testForgotPassword_GeneratesToken() {
        User user = User.builder()
                .id("u1")
                .name("Aarav")
                .email("aarav@example.com")
                .build();
        when(userRepository.findByEmail("aarav@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("aarav@example.com");

        assertNotNull(user.getResetToken());
        assertNotNull(user.getResetTokenExpiry());
        verify(userRepository).save(user);
        verify(deliveryService).sendEmail(eq("aarav@example.com"), anyString(), anyString());
    }

    @Test
    void testResetPassword_Success() {
        User user = User.builder()
                .id("u1")
                .name("Aarav")
                .email("aarav@example.com")
                .resetToken("valid-token")
                .resetTokenExpiry(OffsetDateTime.now().plusHours(1))
                .build();

        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashed");

        authService.resetPassword("valid-token", "newPassword123");

        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpiry());
        assertEquals("newHashed", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void testResetPassword_ExpiredTokenThrows() {
        User user = User.builder()
                .id("u1")
                .resetToken("expired-token")
                .resetTokenExpiry(OffsetDateTime.now().minusMinutes(5))
                .build();

        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

        CustomException ex = assertThrows(CustomException.class, () -> authService.resetPassword("expired-token", "newPassword123"));
        assertEquals("TOKEN_EXPIRED", ex.getCode());
    }

    @Test
    void testAuthenticateUser_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@tracenest.com");
        req.setPassword("SecureP@ss123");

        User user = User.builder()
                .id("user-1")
                .name("Test User")
                .email("test@tracenest.com")
                .role(Role.USER)
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        com.tracenest.backend.Auth.security.UserDetailsImpl principal =
                com.tracenest.backend.Auth.security.UserDetailsImpl.build(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("mock_access_token");

        AuthResponse res = authService.authenticateUser(req);

        assertNotNull(res);
        assertEquals("mock_access_token", res.getAccessToken());
        assertNotNull(res.getRefreshToken());
    }

    @Test
    void testRefreshToken_Success() {
        com.tracenest.backend.Auth.dto.RefreshRequest req = new com.tracenest.backend.Auth.dto.RefreshRequest();
        req.setRefreshToken("valid-refresh-token");

        User user = User.builder()
                .id("user-1")
                .name("Test User")
                .email("test@tracenest.com")
                .role(Role.USER)
                .refreshToken("valid-refresh-token")
                .refreshTokenExpiry(OffsetDateTime.now().plusDays(30))
                .build();
        when(userRepository.findByRefreshToken("valid-refresh-token")).thenReturn(Optional.of(user));
        when(jwtUtils.generateTokenFromUserId("user-1", "USER")).thenReturn("new_access_token");

        AuthResponse res = authService.refreshToken(req);

        assertNotNull(res);
        assertEquals("new_access_token", res.getAccessToken());
        assertEquals("valid-refresh-token", res.getRefreshToken());
    }
}
