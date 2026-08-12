package com.ho.account.ecl.api;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Service] 대손충당금(IFRS9) API 서비스 애플리케이션 진입점.
 *
 * 💡 [교육적 주석: MSA 프로세스 & 모듈 독립성]
 * API 서버는 배치(ecl-batch) 모듈 및 컴포넌트를 스캔하거나 직접 실행하지 않으며,
 * 독립된 Bounded Context 내에서 api 및 core 패키지 전용 빈만 스캔하여 경량을 유지합니다.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.ho.account.ecl.api",
                "com.ho.account.ecl.core"
        })
@EntityScan(basePackages = {
        "com.ho.account.ecl.core.domain",
        "com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa"
})
@EnableJpaRepositories(basePackages =
        "com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa")
@Import(ProductionPostgresqlTlsGuard.class)
public class AllowanceEclApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllowanceEclApiApplication.class, args);
    }
}
