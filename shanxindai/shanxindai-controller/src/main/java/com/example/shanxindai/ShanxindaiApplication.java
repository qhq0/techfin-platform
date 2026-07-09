package com.example.shanxindai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.example.shanxindai")
@EnableJpaRepositories(basePackages = "com.example.shanxindai")
public class ShanxindaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShanxindaiApplication.class, args);
    }
}
