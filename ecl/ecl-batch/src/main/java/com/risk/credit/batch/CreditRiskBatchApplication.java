package com.risk.credit.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 🚀 [Core Application] 신용 리스크 산출 배치 시스템
 * 
 * 💡 [금융 공학 가이드]
 * 본 시스템은 바젤 III(Basel III) 표준방법 및 내부등급법을 지원하기 위한
 * 대량 신용 노출(Exposure) 및 부도율(PD), 부도시손실률(LGD) 산출 배치를 수행합니다.
 */
@SpringBootApplication(scanBasePackages = {"com.risk.credit", "com.risk.common"})
@EntityScan(basePackages = {"com.risk.credit", "com.risk.common.entity"})
@EnableJpaRepositories(basePackages = {"com.risk.credit", "com.risk.common.repository"})
public class CreditRiskBatchApplication {

    public static void main(String[] args) {
        System.setProperty("spring.batch.job.enabled", "false"); 
        SpringApplication.run(CreditRiskBatchApplication.class, args);
    }
}
