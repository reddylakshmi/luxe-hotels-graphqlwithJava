package com.luxe.reservations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.luxe.reservations", "com.luxe.common"})
public class ReservationsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservationsApplication.class, args);
    }
}
