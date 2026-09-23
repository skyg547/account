package com.ho.account.cashflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ho.account.cashflow")
public class CashflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashflowApplication.class, args);
    }
}
