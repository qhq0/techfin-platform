package com.ccb.techfin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ccb.techfin")
@EnableScheduling
public class TechfinApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechfinApplication.class, args);
    }
}
