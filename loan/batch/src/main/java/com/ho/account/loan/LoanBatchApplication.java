package com.ho.account.loan;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 대출계좌 대량 처리 배치 서비스 (Loan Batch Service)
 */
@SpringBootApplication
@EnableBatchProcessing
public class LoanBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanBatchApplication.class, args);
    }
}
