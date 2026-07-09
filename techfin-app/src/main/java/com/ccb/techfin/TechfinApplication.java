package com.ccb.techfin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ccb.techfin")
@EntityScan(basePackages = "com.ccb.techfin")
@EnableJpaRepositories(basePackages = "com.ccb.techfin")
public class TechfinApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechfinApplication.class, args);
    }
}
