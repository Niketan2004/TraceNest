package com.tracenest.backend.Subscription.controller;

import com.tracenest.backend.Notification.service.NotificationDeliveryService;
import com.tracenest.backend.Subscription.dto.SendOtpRequest;
import com.tracenest.backend.Subscription.dto.SubscribeRequest;
import com.tracenest.backend.Subscription.dto.VerifyOtpRequest;
import com.tracenest.backend.Subscription.service.OTPService;
import com.tracenest.backend.Subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public REST controller managing anonymous community alert subscriptions with SMS OTP verification.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Client requests OTP for a mobile number via {@code /send-otp}</li>
 *   <li>Client submits the received 6-digit OTP code to {@code /verify-otp}</li>
 *   <li>Upon validation, client submits contact details and GPS coordinates to {@code /subscribe}</li>
 * </ol>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/subscribe")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final OTPService otpService;
    private final NotificationDeliveryService deliveryService;

    /**
     * Generates a 6-digit one-time password and dispatches it to the recipient's mobile number via SMS.
     *
     * @param request Validated request containing the international mobile number.
     * @return Confirmation response message.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String otp = otpService.generateOtp(request.getMobileNumber());
        String text = "Your TraceNest verification OTP is: " + otp;
        deliveryService.sendSms(request.getMobileNumber(), text);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    /**
     * Validates the supplied 6-digit OTP against the in-memory cache and marks the number verified.
     *
     * @param request Validated request containing mobile number and entered code.
     * @return Confirmation response message.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = otpService.verifyOtp(request.getMobileNumber(), request.getOtp());
        if (isValid) {
            otpService.setVerified(request.getMobileNumber());
            return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
        } else {
            throw new com.tracenest.backend.Common.exception.CustomException(
                    400, "Bad Request", "INVALID_OTP", "Invalid or expired OTP. Please verify your code and try again.");
        }
    }

    /**
     * Completes public subscription registration and dispatches welcome confirmations.
     *
     * @param request Form payload with subscriber name, verified phone, email, and location coordinates.
     * @return Confirmation response message.
     */
    @PostMapping
    public ResponseEntity<?> subscribe(@Valid @RequestBody SubscribeRequest request) {
        subscriptionService.subscribe(request);
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }
}
