package com.luxe.loyalty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.luxe.loyalty", "com.luxe.common"})
public class LoyaltyApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoyaltyApplication.class, args);
    }
}
