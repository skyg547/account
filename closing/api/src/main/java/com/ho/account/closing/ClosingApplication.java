package com.ho.account.closing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.ho.account")
@EnableDiscoveryClient
public class ClosingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClosingApplication.class, args);
    }
}
