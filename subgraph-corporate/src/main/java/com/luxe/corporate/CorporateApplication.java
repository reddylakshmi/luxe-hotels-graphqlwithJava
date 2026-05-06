package com.luxe.corporate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.luxe.corporate", "com.luxe.common"})
public class CorporateApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorporateApplication.class, args);
    }
}
