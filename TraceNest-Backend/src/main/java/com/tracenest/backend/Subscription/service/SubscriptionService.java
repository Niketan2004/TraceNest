package com.tracenest.backend.Subscription.service;

import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Notification.service.NotificationDeliveryService;
import com.tracenest.backend.Notification.service.NotificationService;
import com.tracenest.backend.Report.model.Report;
import com.tracenest.backend.Subscription.dto.CreateSubscriptionRequest;
import com.tracenest.backend.Subscription.dto.SubscribeRequest;
import com.tracenest.backend.Subscription.dto.SubscriptionDto;
import com.tracenest.backend.Subscription.mapper.SubscriptionMapper;
import com.tracenest.backend.Subscription.model.Subscriber;
import com.tracenest.backend.Subscription.model.Subscription;
import com.tracenest.backend.Subscription.repository.SubscriberRepository;
import com.tracenest.backend.Subscription.repository.SubscriptionRepository;
import com.tracenest.backend.User.model.User;
import com.tracenest.backend.User.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service managing geographic alert subscriptions and executing high-precision
 * proximity calculations via the 20KM Haversine spherical distance formula.
 * <p>
 * Supports two tiers of community subscribers:
 * <ol>
 *   <li><b>Public Subscribers</b>: Anonymous community members verified via 6-digit SMS OTP.</li>
 *   <li><b>Authenticated Users</b>: Registered accounts with customized category filters and radius preferences.</li>
 * </ol>
 * </p>
 */
@Service
@Slf4j
public class SubscriptionService {

    private final SubscriberRepository subscriberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationDeliveryService deliveryService;
    private final NotificationService notificationService;
    private final SubscriptionMapper subscriptionMapper;
    private final OTPService otpService;

    /**
     * Constructs the subscription service with necessary repositories and dispatchers.
     */
    public SubscriptionService(
            SubscriberRepository subscriberRepository,
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            NotificationDeliveryService deliveryService,
            NotificationService notificationService,
            SubscriptionMapper subscriptionMapper,
            OTPService otpService) {
        this.subscriberRepository = subscriberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.deliveryService = deliveryService;
        this.notificationService = notificationService;
        this.subscriptionMapper = subscriptionMapper;
        this.otpService = otpService;
    }

    /**
     * Registers a public alert subscriber after verifying the mobile OTP token.
     *
     * @param request Validated public subscription payload.
     * @throws CustomException if the provided phone number is unverified.
     */
    public void subscribe(SubscribeRequest request) {
        if (request.getMobileNumber() != null && !request.getMobileNumber().isBlank()) {
            if (!otpService.isVerified(request.getMobileNumber())) {
                throw new CustomException(400, "Bad Request", "OTP_NOT_VERIFIED", "Mobile number is not verified");
            }
        }

        Subscriber subscriber = subscriberRepository.findByEmailOrMobileNumber(request.getEmail(), request.getMobileNumber())
                .orElse(new Subscriber());

        subscriber.setFullName(request.getFullName());
        subscriber.setEmail(request.getEmail());
        subscriber.setMobileNumber(request.getMobileNumber());
        subscriber.setCity(request.getCity());
        subscriber.setLat(request.getLat());
        subscriber.setLng(request.getLng());
        subscriber.setIsVerified(true);
        subscriber.setVerifiedAt(OffsetDateTime.now());

        subscriberRepository.save(subscriber);
        log.info("Saved public subscriber: {}", subscriber.getEmail() != null ? subscriber.getEmail() : subscriber.getMobileNumber());

        // Send welcome email
        if (subscriber.getEmail() != null && !subscriber.getEmail().isBlank()) {
            String subject = "Welcome to TraceNest Community Alerts — Subscription Confirmed";
            String body = String.format(
                    "Hi %s,\n\nThank you for subscribing to TraceNest local emergency alerts.\n\n" +
                    "Monitored City: %s\nRadius: 20 KM\n\n" +
                    "Whenever a missing person, lost pet, or stolen vehicle is reported near your area, you will receive an immediate notification.\n\n" +
                    "— TraceNest Community Safety\nhttps://tracenest.app",
                    subscriber.getFullName(),
                    subscriber.getCity() != null ? subscriber.getCity() : "Nearby radius"
            );
            deliveryService.sendEmail(subscriber.getEmail(), subject, body);
        }

        // Send welcome SMS
        if (subscriber.getMobileNumber() != null && !subscriber.getMobileNumber().isBlank()) {
            String sms = String.format("TraceNest: You are now subscribed to local alerts for %s. We will notify you when emergency incidents occur near you.",
                    subscriber.getCity() != null ? subscriber.getCity() : "your area");
            deliveryService.sendSms(subscriber.getMobileNumber(), sms);
        }
    }

    /**
     * Lists active subscriptions configured for an authenticated user.
     *
     * @param userId Authenticated user primary key.
     * @return List of mapped {@link SubscriptionDto} records.
     */
    public List<SubscriptionDto> getUserSubscriptions(String userId) {
        List<Subscription> subs = subscriptionRepository.findByUserId(userId);
        return subs.stream().map(subscriptionMapper::toDto).toList();
    }

    /**
     * Creates a customized geographic alert subscription for an authenticated account.
     *
     * @param userId Authenticated user ID.
     * @param request Target parameters including categories, city, coordinates, and radius.
     * @return Created {@link SubscriptionDto}.
     */
    public SubscriptionDto createSubscription(String userId, CreateSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "User not found"));

        Subscription sub = Subscription.builder()
                .user(user)
                .city(request.getCity())
                .types(request.getTypes() != null && !request.getTypes().isEmpty() ? request.getTypes() : List.of("all"))
                .lat(request.getLat())
                .lng(request.getLng())
                .radiusKm(request.getRadiusKm() != null ? request.getRadiusKm() : 20.0)
                .build();

        sub = subscriptionRepository.save(sub);
        log.info("Created subscription {} for user {}", sub.getId(), userId);

        // Notify user via in-app notification
        try {
            notificationService.createNotification(
                    user,
                    "SUBSCRIPTION_CONFIRMED",
                    "Alert Area Created: " + sub.getCity(),
                    "You will receive emergency alerts for incidents occurring within " + sub.getRadiusKm() + "km of " + sub.getCity() + ".",
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to create in-app notification for subscription {}: {}", sub.getId(), e.getMessage());
        }

        // Send confirmation email
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                String subject = "TraceNest: Alert Subscription Confirmed";
                String body = String.format(
                        "Hi %s,\n\nYour alert subscription for %s has been confirmed.\nRadius: %.1f km\n\n— TraceNest Team",
                        user.getName(), sub.getCity(), sub.getRadiusKm()
                );
                deliveryService.sendEmail(user.getEmail(), subject, body);
            } catch (Exception e) {
                log.warn("Failed to send subscription confirmation email: {}", e.getMessage());
            }
        }

        return subscriptionMapper.toDto(sub);
    }

    /**
     * Updates an authenticated user's existing subscription.
     *
     * @param userId Authenticated user ID.
     * @param subId Subscription identifier.
     * @param request Update payload.
     * @return Updated {@link SubscriptionDto}.
     */
    public SubscriptionDto updateSubscription(String userId, String subId, CreateSubscriptionRequest request) {
        Subscription sub = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Subscription not found"));

        if (!sub.getUser().getId().equals(userId)) {
            throw new CustomException(403, "Forbidden", "NOT_OWNER", "You do not own this subscription");
        }

        if (request.getTypes() != null) sub.setTypes(request.getTypes());
        if (request.getCity() != null) sub.setCity(request.getCity());
        if (request.getRadiusKm() != null) sub.setRadiusKm(request.getRadiusKm());
        if (request.getLat() != null) sub.setLat(request.getLat());
        if (request.getLng() != null) sub.setLng(request.getLng());

        sub = subscriptionRepository.save(sub);
        return subscriptionMapper.toDto(sub);
    }

    /**
     * Deletes a subscription for an authenticated user.
     *
     * @param userId Authenticated user ID.
     * @param subId Subscription identifier.
     */
    public void deleteSubscription(String userId, String subId) {
        Subscription sub = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Subscription not found"));

        if (!sub.getUser().getId().equals(userId)) {
            throw new CustomException(403, "Forbidden", "NOT_OWNER", "You do not own this subscription");
        }

        subscriptionRepository.delete(sub);
        log.info("Deleted subscription {} for user {}", subId, userId);
    }

    /**
     * Executes the Haversine radius evaluation and dispatches multi-channel alerts (in-app, email, SMS)
     * to all registered and public subscribers located within proximity of the incident.
     *
     * @param report Newly created or updated report entity.
     * @param isCityWide Whether the incident qualifies for city-wide broadcast.
     */
    public void notifySubscribers(Report report, boolean isCityWide) {
        log.info("Notifying subscribers for report {}. City-wide: {}", report.getId(), isCityWide);

        Double reportLat = report.getLocation() != null ? report.getLocation().getLat() : null;
        Double reportLng = report.getLocation() != null ? report.getLocation().getLng() : null;
        String reportCity = report.getLocation() != null && report.getLocation().getCity() != null
                ? report.getLocation().getCity().trim() : null;
        String reportAddress = report.getLocation() != null && report.getLocation().getAddress() != null
                ? report.getLocation().getAddress().toLowerCase() : "";
        String reportTypeStr = report.getType() != null
                ? report.getType().name().toLowerCase().replace("_", "-") : "";
        String reporterId = report.getReporter() != null ? report.getReporter().getId() : null;

        // -------------------------------------------------------------------------
        // 1. Process Authenticated User Subscriptions
        // -------------------------------------------------------------------------
        List<Subscription> userSubs = subscriptionRepository.findAll();
        for (Subscription sub : userSubs) {
            // Do not notify the person who filed the report
            if (reporterId != null && sub.getUser() != null && reporterId.equals(sub.getUser().getId())) {
                continue;
            }

            // Category/Type Matching (supports "all", hyphens, underscores, case-insensitivity)
            if (sub.getTypes() != null && !sub.getTypes().isEmpty()) {
                boolean typeMatches = sub.getTypes().stream().anyMatch(t ->
                        t.equalsIgnoreCase("all") ||
                        t.equalsIgnoreCase(reportTypeStr) ||
                        t.equalsIgnoreCase(report.getType().name()) ||
                        t.replace("-", "_").equalsIgnoreCase(report.getType().name())
                );
                if (!typeMatches) continue;
            }

            // Geo / Area Matching
            boolean shouldNotify = isSubscriberInArea(
                    sub.getLat(), sub.getLng(), sub.getCity(),
                    sub.getRadiusKm() != null ? sub.getRadiusKm() : 20.0,
                    reportLat, reportLng, reportCity, reportAddress, isCityWide
            );

            if (shouldNotify && sub.getUser() != null) {
                // In-app notification
                try {
                    String title = "New " + report.getType().name().replace("_", " ").toLowerCase() + " reported near you";
                    String body = report.getTitle() + (reportCity != null ? " (" + reportCity + ")" : "") + "\n" +
                            (report.getDescription() != null ? report.getDescription() : "");
                    notificationService.createNotification(
                            sub.getUser(),
                            "NEW_REPORT_NEARBY",
                            title,
                            body,
                            report.getId()
                    );
                } catch (Exception e) {
                    log.error("Failed to create in-app alert for user {}", sub.getUser().getId(), e);
                }

                // Email notification
                if (sub.getUser().getEmail() != null && !sub.getUser().getEmail().isBlank()) {
                    String subject = "TraceNest Emergency Alert: " + report.getTitle();
                    String body = String.format(
                            "Hi %s,\n\nA new %s report has been filed in your monitored area.\n\n" +
                            "Title: %s\n" +
                            "Location: %s\n" +
                            "Description: %s\n" +
                            "Contact: %s\n\n" +
                            "Please check TraceNest immediately to view full details and help:\n" +
                            "https://tracenest.app/reports/%s\n\n" +
                            "— TraceNest Community Safety",
                            sub.getUser().getName(),
                            report.getType().name().replace("_", " "),
                            report.getTitle(),
                            reportCity != null ? reportCity : "Nearby",
                            report.getDescription(),
                            report.getContact() != null ? report.getContact() : "See website",
                            report.getId()
                    );
                    deliveryService.sendEmail(sub.getUser().getEmail(), subject, body);
                }

                // SMS notification (if user has phone number)
                if (sub.getUser().getPhone() != null && !sub.getUser().getPhone().isBlank()) {
                    String smsText = String.format("TraceNest Alert: %s reported in %s: '%s'. Check https://tracenest.app/reports/%s",
                            report.getType().name().replace("_", " "),
                            reportCity != null ? reportCity : "your area",
                            report.getTitle(),
                            report.getId());
                    deliveryService.sendSms(sub.getUser().getPhone(), smsText);
                }
            }
        }

        // -------------------------------------------------------------------------
        // 2. Process Public Anonymous Subscribers
        // -------------------------------------------------------------------------
        List<Subscriber> subscribers = subscriberRepository.findAll();
        for (Subscriber sub : subscribers) {
            boolean shouldNotify = isSubscriberInArea(
                    sub.getLat(), sub.getLng(), sub.getCity(),
                    20.0,
                    reportLat, reportLng, reportCity, reportAddress, isCityWide
            );

            if (shouldNotify) {
                sendPublicNotification(sub, report, reportCity);

                // If public subscriber email matches a registered user, also create in-app notification
                if (sub.getEmail() != null) {
                    userRepository.findByEmail(sub.getEmail()).ifPresent(user -> {
                        try {
                            notificationService.createNotification(
                                    user,
                                    "NEW_REPORT_NEARBY",
                                    "Alert near you: " + report.getTitle(),
                                    report.getDescription(),
                                    report.getId()
                            );
                        } catch (Exception e) {
                            // ignore duplicate
                        }
                    });
                }
            }
        }
    }

    /**
     * Determines whether a subscriber's registered location coordinates fall within the
     * proximity threshold of a reported incident using the Haversine formula and city fallbacks.
     */
    private boolean isSubscriberInArea(
            Double subLat, Double subLng, String subCity, double radiusKm,
            Double repLat, Double repLng, String repCity, String repAddress, boolean isCityWide) {

        // 1. Exact or contains city matching (reliable fallback)
        if (subCity != null && !subCity.isBlank()) {
            String cleanSubCity = subCity.trim().toLowerCase();
            if (repCity != null && repCity.toLowerCase().equals(cleanSubCity)) {
                return true;
            }
            if (repAddress != null && repAddress.contains(cleanSubCity)) {
                return true;
            }
        }

        // 2. Haversine GPS radius check when coordinates are present
        if (subLat != null && subLng != null && repLat != null && repLng != null) {
            double distance = calculateDistanceInKm(subLat, subLng, repLat, repLng);
            double effectiveRadius = isCityWide ? Math.max(50.0, radiusKm) : radiusKm;
            if (distance <= effectiveRadius) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper to dispatch email and SMS notifications to a verified public subscriber.
     */
    private void sendPublicNotification(Subscriber sub, Report report, String reportCity) {
        String subject = "TraceNest Emergency Alert: " + report.getTitle();
        String text = String.format(
                "Hi %s,\n\nA new %s report has been filed near your area (%s).\n\n" +
                "Title: %s\n" +
                "Description: %s\n" +
                "Contact: %s\n\n" +
                "Please visit https://tracenest.app/reports/%s to view details.\n\n" +
                "— TraceNest Community Safety",
                sub.getFullName(),
                report.getType().name().replace("_", " "),
                reportCity != null ? reportCity : (sub.getCity() != null ? sub.getCity() : "your area"),
                report.getTitle(),
                report.getDescription(),
                report.getContact() != null ? report.getContact() : "See website",
                report.getId()
        );

        if (sub.getEmail() != null && !sub.getEmail().isBlank()) {
            deliveryService.sendEmail(sub.getEmail(), subject, text);
        }

        if (sub.getMobileNumber() != null && !sub.getMobileNumber().isBlank()) {
            String smsText = String.format("TraceNest Alert: %s reported near %s: '%s'. Details: https://tracenest.app/reports/%s",
                    report.getType().name().replace("_", " "),
                    reportCity != null ? reportCity : "your area",
                    report.getTitle(),
                    report.getId());
            deliveryService.sendSms(sub.getMobileNumber(), smsText);
        }
    }

    /**
     * High-precision implementation of the Haversine spherical trigonometric formula
     * calculating the great-circle distance between two GPS coordinates in kilometers.
     *
     * @param lat1 Subscriber latitude in degrees.
     * @param lon1 Subscriber longitude in degrees.
     * @param lat2 Incident latitude in degrees.
     * @param lon2 Incident longitude in degrees.
     * @return Distance between the two points across Earth's surface in kilometers.
     */
    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Mean radius of the earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
