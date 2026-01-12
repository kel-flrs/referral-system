package com.bullhorn.mockservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configuration for webhook functionality
 * Sets up RestTemplate, async executor, and scheduling
 */
@Configuration
@EnableAsync
@EnableScheduling
public class WebhookConfig {

    /**
     * RestTemplate specifically configured for webhook delivery
     * Includes timeouts and error handling
     */
    @Bean(name = "webhookRestTemplate")
    public RestTemplate webhookRestTemplate(RestTemplateBuilder builder) {
        return builder
            .connectTimeout(Duration.ofSeconds(10))  // Connection timeout
            .readTimeout(Duration.ofSeconds(30))     // Read timeout (matches default webhook timeout)
            .build();
    }

    /**
     * Dedicated thread pool for async webhook delivery
     * Prevents webhook processing from blocking main application threads
     */
    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: minimum threads always alive
        executor.setCorePoolSize(5);

        // Max pool size: maximum threads during high load
        executor.setMaxPoolSize(20);

        // Queue capacity: tasks waiting when all threads are busy
        executor.setQueueCapacity(500);

        // Thread naming for easier debugging
        executor.setThreadNamePrefix("webhook-");

        // Rejection policy: caller runs if queue is full (backpressure)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
