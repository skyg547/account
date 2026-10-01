package com.ho.account.closing.batch;

import com.ho.account.closing.application.pipeline.FxValuationPipeline;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.EclProvisionService;
import com.ho.account.closing.application.service.FinancialClosingCalculationService;
import com.ho.account.closing.application.service.FxValuationService;
import com.ho.account.closing.application.service.FxValuationEligibilityResolver;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import com.ho.account.closing.infrastructure.local.ClosingLocalExternalPortConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ComponentScan(basePackages = "com.ho.account.closing.batch", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                JournalFxValuationBalanceSource.class
        })
})
@EntityScan(basePackages = {
        "com.ho.account.closing.domain",
        "com.ho.account.closing.infrastructure.persistence"
})
@EnableConfigurationProperties(ClosingAccountingProperties.class)
@Import({FxValuationEligibilityResolver.class, FxValuationService.class, FxValuationPipeline.class,
        EclProvisionService.class, FinancialClosingCalculationService.class,
        ClosingLocalExternalPortConfiguration.class})
public class ClosingBatchApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ClosingBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = application.run(args);
        // The Boot runner has completed synchronously; propagate Batch failures and close pools.
        if (context.getEnvironment().getProperty("spring.batch.job.enabled", Boolean.class, true)
                && !context.getEnvironment().getProperty("spring.batch.job.name", "").isBlank()) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
