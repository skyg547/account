package com.ho.account.closing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account.closing")
@EntityScan(basePackages = {
        "com.ho.account.closing.domain",
        "com.ho.account.closing.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = "com.ho.account.closing.infrastructure.persistence")
@EnableDiscoveryClient
public class ClosingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClosingApplication.class, args);
    }
}
