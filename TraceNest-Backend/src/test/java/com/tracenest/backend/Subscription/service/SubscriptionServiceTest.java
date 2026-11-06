package com.tracenest.backend.Subscription.service;

import com.tracenest.backend.Notification.service.NotificationDeliveryService;
import com.tracenest.backend.Notification.service.NotificationService;
import com.tracenest.backend.Report.model.Location;
import com.tracenest.backend.Report.model.Report;
import com.tracenest.backend.Report.model.ReportStatus;
import com.tracenest.backend.Report.model.ReportType;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationDeliveryService deliveryService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private OTPService otpService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void testCreateSubscription_Success() {
        User user = User.builder().id("u1").name("Aarav").email("test@example.com").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        CreateSubscriptionRequest req = CreateSubscriptionRequest.builder()
                .city("Bengaluru")
                .types(List.of("pet", "person"))
                .radiusKm(15.0)
                .lat(12.9716)
                .lng(77.5946)
                .build();

        Subscription saved = Subscription.builder()
                .id("sub-1")
                .city("Bengaluru")
                .types(List.of("pet", "person"))
                .radiusKm(15.0)
                .user(user)
                .build();
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
        when(subscriptionMapper.toDto(saved)).thenReturn(SubscriptionDto.builder().id("sub-1").city("Bengaluru").build());

        SubscriptionDto res = subscriptionService.createSubscription("u1", req);

        assertNotNull(res);
        assertEquals("sub-1", res.getId());
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(notificationService).createNotification(eq(user), eq("SUBSCRIPTION_CONFIRMED"), anyString(), anyString(), isNull());
        verify(deliveryService).sendEmail(eq("test@example.com"), contains("Alert Subscription Confirmed"), anyString());
    }

    @Test
    void testSubscribePublic_SendsWelcomeEmailAndSms() {
        SubscribeRequest req = new SubscribeRequest();
        req.setFullName("Pooja Sharma");
        req.setEmail("pooja@example.com");
        req.setMobileNumber("+919876543210");
        req.setCity("Pune");
        req.setAddress("Kothrud, Pune");
        req.setLat(18.5074);
        req.setLng(73.8077);

        when(otpService.isVerified("+919876543210")).thenReturn(true);
        when(userRepository.findByEmail("pooja@example.com")).thenReturn(Optional.empty());

        subscriptionService.subscribe(req);

        verify(subscriberRepository).save(any(Subscriber.class));
        verify(deliveryService).sendEmail(eq("pooja@example.com"), contains("Welcome to TraceNest Community Alerts"), anyString());
        verify(deliveryService).sendSms(eq("+919876543210"), contains("TraceNest"));
    }

    @Test
    void testNotifySubscribers_Within20KmTriggersNotification() {
        // Report at Bengaluru center (12.9716, 77.5946)
        Report report = Report.builder()
                .id("r1")
                .type(ReportType.PET)
                .title("Lost Labrador")
                .description("Yellow lab missing")
                .status(ReportStatus.MISSING)
                .location(Location.builder().lat(12.9716).lng(77.5946).city("Bengaluru").build())
                .reporter(User.builder().id("reporter-1").build())
                .build();

        // User A subscribed at 12.95, 77.60 (distance ~3 km - within 20km)
        User subUser = User.builder().id("user-a").name("Sub User").email("sub@example.com").build();
        Subscription userSub = Subscription.builder()
                .id("s1")
                .city("Bengaluru")
                .types(List.of("pet"))
                .radiusKm(20.0)
                .lat(12.95)
                .lng(77.60)
                .user(subUser)
                .build();

        when(subscriptionRepository.findAll()).thenReturn(List.of(userSub));
        when(subscriberRepository.findAll()).thenReturn(List.of());

        subscriptionService.notifySubscribers(report, false);

        // Verify in-app notification created
        verify(notificationService).createNotification(
                eq(subUser),
                eq("NEW_REPORT_NEARBY"),
                anyString(),
                anyString(),
                eq("r1")
        );

        // Verify email sent
        verify(deliveryService).sendEmail(eq("sub@example.com"), anyString(), anyString());
    }

    @Test
    void testNotifySubscribers_MatchingCityWithoutCoordinates_TriggersNotification() {
        // Report filed with city "Pune", without GPS coords
        Report report = Report.builder()
                .id("r2")
                .type(ReportType.PERSON)
                .title("Missing Elderly Person")
                .description("Last seen near Swargate")
                .status(ReportStatus.MISSING)
                .location(Location.builder().city("Pune").address("Swargate, Pune").build())
                .reporter(User.builder().id("reporter-99").build())
                .build();

        // User subscribed to Pune
        User subUser = User.builder().id("user-pune").name("Pune Resident").email("pune@example.com").build();
        Subscription userSub = Subscription.builder()
                .id("s-pune")
                .city("pune")
                .types(List.of("all"))
                .radiusKm(20.0)
                .user(subUser)
                .build();

        when(subscriptionRepository.findAll()).thenReturn(List.of(userSub));
        when(subscriberRepository.findAll()).thenReturn(List.of());

        subscriptionService.notifySubscribers(report, false);

        verify(notificationService).createNotification(
                eq(subUser),
                eq("NEW_REPORT_NEARBY"),
                anyString(),
                anyString(),
                eq("r2")
        );
        verify(deliveryService).sendEmail(eq("pune@example.com"), contains("Emergency Alert"), anyString());
    }

    @Test
    void testNotifySubscribers_FarAwayDoesNotTrigger() {
        // Report at Bengaluru (12.9716, 77.5946)
        Report report = Report.builder()
                .id("r1")
                .type(ReportType.PET)
                .title("Lost Labrador")
                .status(ReportStatus.MISSING)
                .location(Location.builder().lat(12.9716).lng(77.5946).city("Bengaluru").build())
                .reporter(User.builder().id("reporter-1").build())
                .build();

        // User B subscribed in Mumbai (19.0760, 72.8777) - distance ~840 km
        User farUser = User.builder().id("user-b").email("far@example.com").build();
        Subscription farSub = Subscription.builder()
                .id("s2")
                .city("Mumbai")
                .types(List.of("pet"))
                .radiusKm(20.0)
                .lat(19.0760)
                .lng(72.8777)
                .user(farUser)
                .build();

        when(subscriptionRepository.findAll()).thenReturn(List.of(farSub));
        when(subscriberRepository.findAll()).thenReturn(List.of());

        subscriptionService.notifySubscribers(report, false);

        // Neither notification nor email should be sent
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        verify(deliveryService, never()).sendEmail(any(), any(), any());
    }
}
