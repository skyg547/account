package com.ho.account.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ho.account.reporting")
public class ReportingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingApiApplication.class, args);
    }
}
