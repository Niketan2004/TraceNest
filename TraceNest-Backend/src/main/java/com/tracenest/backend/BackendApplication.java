package com.tracenest.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the TraceNest Backend REST API service.
 * <p>
 * TraceNest is a full-stack, community-driven emergency intelligence and recovery platform
 * designed to report, track, and locate missing persons, lost pets, and stolen/misplaced vehicles.
 * </p>
 * <p>
 * Key capabilities initialized here:
 * <ul>
 *   <li>Exclusion of default auto-configured {@link UserDetailsServiceAutoConfiguration} to enable
 *       custom database-backed user authentication.</li>
 *   <li>{@link EnableAsync} activation for non-blocking email, SMS, and geolocation fan-out.</li>
 * </ul>
 * </p>
 */
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableAsync
public class BackendApplication {

    /**
     * Boots the Spring Boot application context.
     *
     * @param args Command-line execution arguments passed to the application.
     */
    public static void main(String[] args) {
        System.setProperty("spring.classformat.ignore", "true");
        SpringApplication.run(BackendApplication.class, args);
    }

}
