package com.luxe.experiences;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.luxe.experiences", "com.luxe.common"})
public class ExperiencesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExperiencesApplication.class, args);
    }
}
