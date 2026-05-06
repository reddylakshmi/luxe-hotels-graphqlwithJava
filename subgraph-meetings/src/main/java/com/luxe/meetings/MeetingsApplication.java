package com.luxe.meetings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.luxe.meetings", "com.luxe.common"})
public class MeetingsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingsApplication.class, args);
    }
}
