package com.ho.account.closing.application.service;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FinalCloseEvidenceTimeConfiguration {

    @Bean("finalCloseEvidenceClock")
    Clock finalCloseEvidenceClock() {
        return Clock.systemUTC();
    }
}
