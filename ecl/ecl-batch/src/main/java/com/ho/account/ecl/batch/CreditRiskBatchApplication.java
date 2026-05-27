package com.ho.account.ecl.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 🚀 [Core Application] 대손충당금(IFRS9) 산출 배치 시스템
 * 
 * 💡 [금융 공학 가이드]
 * 본 시스템은 국제 금융 규제(국제 금융 규제) 표준방법 및 내부등급법을 지원하기 위한
 * 대량 신용 노출(Exposure) 및 부도율(PD), 부도시손실률(LGD) 산출 배치를 수행합니다.
 */
@SpringBootApplication(scanBasePackages = {"com.ho.account.ecl", "com.ho.account.shared.finance"})
@EntityScan(basePackages = {"com.ho.account.ecl", "com.ho.account.shared.finance.entity"})
@EnableJpaRepositories(basePackages = {"com.ho.account.ecl", "com.ho.account.shared.finance.repository"})
public class CreditRiskBatchApplication {

    public static void main(String[] args) {
        System.setProperty("spring.batch.job.enabled", "false"); 
        SpringApplication.run(CreditRiskBatchApplication.class, args);
    }
}
