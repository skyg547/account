package com.ho.account.cashflow.batch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackages = "com.ho.account.cashflow")
public class CashflowBatchApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(CashflowBatchApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
