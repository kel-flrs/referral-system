package com.bullhorn.mockservice.config;

import com.bullhorn.mockservice.service.MockDataGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the database with mock data on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MockDataGeneratorService mockDataGeneratorService;

    @Value("${app.mock-data.enable-auto-generation:true}")
    private boolean enableAutoGeneration;

    @Value("${app.mock-data.default-count:1000}")
    private int defaultCount;

    @Override
    public void run(ApplicationArguments args) {
        if (!enableAutoGeneration) {
            log.info("Mock data auto-generation is disabled");
            return;
        }

        log.info("Starting mock data initialization...");

        try {
            mockDataGeneratorService.initializeMockData(
                    500000,  // candidates
                    500000,           // consultants
                    200            // job orders
            );
            log.info("Mock data initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize mock data", e);
        }
    }
}
