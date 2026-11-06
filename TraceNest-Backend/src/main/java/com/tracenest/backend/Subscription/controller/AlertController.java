package com.tracenest.backend.Subscription.controller;

import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Subscription.dto.CreateSubscriptionRequest;
import com.tracenest.backend.Subscription.dto.SubscriptionDto;
import com.tracenest.backend.Subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing geographic alert subscriptions for authenticated accounts.
 * <p>
 * Allows registered users to configure custom radius boundaries and categories
 * for emergency in-app and email/SMS alerts.
 * </p>
 */
@RestController
@RequestMapping({"/api/v1/subscriptions", "/api/v1/alerts/subscriptions"})
@RequiredArgsConstructor
public class AlertController {

    private final SubscriptionService subscriptionService;

    /**
     * Creates a new personalized geographic alert subscription.
     *
     * @param userDetails Authenticated user principal.
     * @param request Validated subscription parameters.
     * @return Created {@link SubscriptionDto} with HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<SubscriptionDto> createSubscription(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionDto created = subscriptionService.createSubscription(userDetails.getId(), request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Lists all alert subscriptions registered to the authenticated account.
     *
     * @param userDetails Authenticated user principal.
     * @return Map containing array of {@link SubscriptionDto}.
     */
    @GetMapping
    public ResponseEntity<Map<String, List<SubscriptionDto>>> getSubscriptions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<SubscriptionDto> items = subscriptionService.getUserSubscriptions(userDetails.getId());
        return ResponseEntity.ok(Map.of("items", items));
    }

    /**
     * Modifies the radius, location, or categories of an existing alert subscription.
     *
     * @param userDetails Authenticated user principal.
     * @param id Subscription primary key.
     * @param request Partial update payload.
     * @return Updated {@link SubscriptionDto}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SubscriptionDto> updateSubscription(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id,
            @RequestBody CreateSubscriptionRequest request) {
        SubscriptionDto updated = subscriptionService.updateSubscription(userDetails.getId(), id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Removes an alert subscription.
     *
     * @param userDetails Authenticated user principal.
     * @param id Subscription identifier.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        subscriptionService.deleteSubscription(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
