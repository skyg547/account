package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AssetLeaseIntegrationFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(LeasePaymentResolutionPort.class)
    public LeasePaymentResolutionPort unavailableLeasePaymentResolutionPort() {
        return new UnavailableLeasePaymentResolutionAdapter();
    }
}
