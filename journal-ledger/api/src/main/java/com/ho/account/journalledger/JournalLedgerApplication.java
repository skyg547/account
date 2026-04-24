package com.ho.account.journalledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Journal Ledger 마이크로서비스 애플리케이션 메인 클래스
 */
@SpringBootApplication(scanBasePackages = "com.ho.account")
@EnableDiscoveryClient
@EnableFeignClients // 다른 마이크로서비스 API 호출(FeignClient) 기능 활성화
public class JournalLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerApplication.class, args);
    }

}
