package com.tracenest.backend.Notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous notification delivery service handling multi-channel dispatches
 * via Spring Mail (SMTP/Gmail) and Twilio SDK (SMS emergency text messaging).
 * <p>
 * Gracefully handles mock/placeholder credentials during local development and testing,
 * allowing the platform to boot and run offline.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Initializes the Twilio client upon Spring bean construction if live credentials exist.
     */
    @PostConstruct
    public void init() {
        if (!"mock_sid".equals(twilioAccountSid) && twilioAccountSid != null && !twilioAccountSid.isEmpty()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio is running with mock/placeholder credentials");
        }
    }

    /**
     * Asynchronously delivers an email notification via configured SMTP mail server.
     *
     * @param to Recipient email address.
     * @param subject Email subject header.
     * @param text Email body content.
     */
    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Sent email to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    /**
     * Asynchronously delivers an SMS alert via Twilio REST API.
     *
     * @param to Destination mobile number with international dialing prefix (e.g., +15551234567).
     * @param text SMS body message.
     */
    @Async
    public void sendSms(String to, String text) {
        try {
            if ("mock_sid".equals(twilioAccountSid) || twilioAccountSid == null || twilioAccountSid.isEmpty()) {
                log.info("MOCK SMS to {}: {}", to, text);
                return;
            }
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioPhoneNumber.trim()),
                    text
            ).create();
            log.info("Sent SMS to {}. SID: {}", to, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", to, e);
        }
    }
}
