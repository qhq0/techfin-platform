package com.ccb.techfin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ccb.techfin")
public class TechfinApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechfinApplication.class, args);
    }
}
