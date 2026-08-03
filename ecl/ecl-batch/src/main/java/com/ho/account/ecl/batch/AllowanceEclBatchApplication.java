package com.ho.account.ecl.batch;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 🚀 [Core Application] 대손충당금(IFRS9) 산출 배치 시스템
 * 
 * 💡 [금융 공학 가이드]
 * 본 시스템은 IFRS 9 대손충당금 결산을 위한 대량 신용 노출(Exposure),
 * 부도율(PD), 부도시손실률(LGD), 기대신용손실(ECL) 산출 배치를 수행합니다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.ecl")
@EntityScan(basePackages = {
        "com.ho.account.ecl.core.domain",
        "com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa"
})
@EnableJpaRepositories(basePackages =
        "com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa")
@Import(ProductionPostgresqlTlsGuard.class)
public class AllowanceEclBatchApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AllowanceEclBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of("spring.batch.job.enabled", "false"));

        // [초보 가이드] 기본 bootRun은 컨텍스트만 확인하고, 실제 Job 실행은 CLI 인자로 명시합니다.
        ConfigurableApplicationContext context = application.run(args);
        if (containsJobName(args)) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
    }

    private static boolean containsJobName(String[] args) {
        for (String arg : args) {
            if (arg.contains("spring.batch.job.name") || arg.contains("job.name")) {
                return true;
            }
        }
        return false;
    }
}
