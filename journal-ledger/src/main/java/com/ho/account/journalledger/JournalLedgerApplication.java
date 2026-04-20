package com.ho.account.journalledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.ho.account")
@EnableDiscoveryClient
public class JournalLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerApplication.class, args);
    }

}
