package com.ho.account.journalledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.ho.account")
@EnableDiscoveryClient
@EnableFeignClients // ?¤ë¥¸ ë§ˆì´?¬ë¡œ?œë¹„?¤ì? ?°ì•„?˜ê²Œ ?„í™”(API ?¸ì¶œ)?????ˆëŠ” ê¸°ëŠ¥??ì¼?‹ˆ??
public class JournalLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerApplication.class, args);
    }

}
