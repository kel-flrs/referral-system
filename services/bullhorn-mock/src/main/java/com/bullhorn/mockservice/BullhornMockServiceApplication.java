package com.bullhorn.mockservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BullhornMockServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BullhornMockServiceApplication.class, args);
    }
}
