package com.ho.account.journalledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.ho.account")
@EnableDiscoveryClient
@EnableFeignClients // 다른 마이크로서비스와 우아하게 전화(API 호출)할 수 있는 기능을 켭니다.
public class JournalLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerApplication.class, args);
    }

}
