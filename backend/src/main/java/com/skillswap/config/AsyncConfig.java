package com.skillswap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async}. Methods run on Spring Boot's auto-configured {@code applicationTaskExecutor}, sized through
 * the {@code spring.task.execution.*} properties.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
