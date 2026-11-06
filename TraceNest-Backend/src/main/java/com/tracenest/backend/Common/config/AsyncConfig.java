package com.tracenest.backend.Common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring asynchronous processing configuration.
 * <p>
 * Enables execution of {@code @Async} annotated methods on dedicated thread pools,
 * ensuring non-blocking operations for email dispatch, SMS alerting, and image pruning.
 * </p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
