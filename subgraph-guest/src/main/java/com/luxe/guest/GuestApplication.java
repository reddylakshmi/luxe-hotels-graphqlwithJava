package com.luxe.guest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.luxe.guest", "com.luxe.common"})
public class GuestApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuestApplication.class, args);
    }
}
