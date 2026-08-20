package com.ho.account.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [대출계좌 대량 처리 배치 서비스 (Loan Batch Service Main Application)]
 *
 * <p><strong>Pedagogical Explanation & Architecture Design:</strong></p>
 * <ul>
 *   <li><strong>Non-Web Process Execution Structure (비 웹 프로세스 구동)</strong>:
 *       배치 모듈은 HTTP API 요청을 수신하는 웹 서버(Tomcat 등)가 아니므로
 *       {@link WebApplicationType#NONE}을 명시적으로 설정하여 서블릿 컨테이너 구동 비용을 제거합니다.</li>
 *   <li><strong>Resource Automatic Release & Exit Code Handling (자원 반납 및 종료 처리)</strong>:
 *       CLI로 특정 Job 실행 요청(예: {@code --spring.batch.job.name=loanInterestAccrualJob})이 포함된 경우,
 *       작업 수행 완료 후 {@link SpringApplication#exit(ConfigurableApplicationContext)} 및 {@link System#exit(int)}를
 *       호출하여 JVM 및 시스템 메모리 자원을 즉시 자동 반납하고 셧다운 생명주기를 완료합니다.</li>
 *   <li><strong>Spring Boot 3 Batch Auto-Configuration & Meta-schema Setup</strong>:
 *       Spring Boot 3에서는 {@code @EnableBatchProcessing}을 붙이면 {@code BatchAutoConfiguration}이 비활성화되므로
 *       이를 제거하여 H2 데이터베이스에 배치 메타데이터 테이블이 자동으로 생성되도록 보장합니다.</li>
 *   <li><strong>Bounded Context Isolation (거버넌스 웹 컨트롤러 노출 차단)</strong>:
 *       {@code shared-kernel}의 {@code AuditController} 등 거버넌스 REST 컨트롤러가 배치 프로세스에 로드되지 않도록
 *       {@link ComponentScan.Filter}를 통해 {@code AuditController}를 컴포넌트 스캔에서 명시적으로 제외합니다.</li>
 * </ul>
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.ho.account.loan",
                "com.ho.account.journalledger",
                "com.ho.account.common",
                "com.ho.account.shared",
                "com.ho.account.masterdata.core"
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.ho\\.account\\.shared\\.infrastructure\\.security\\.web\\..*"
                )
        }
)
@EntityScan(basePackages = {
        "com.ho.account.loan.domain",
        "com.ho.account.journalledger.domain",
        "com.ho.account.masterdata.core.domain",
        "com.ho.account.masterdata.core.infrastructure.persistence",
        "com.ho.account.shared.infrastructure.security.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.loan.infrastructure.persistence",
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence",
        "com.ho.account.shared.infrastructure.security.repository"
})
public class LoanBatchApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LoanBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        ConfigurableApplicationContext context = application.run(args);

        if (containsJobName(args)) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
    }

    private static boolean containsJobName(String[] args) {
        for (String arg : args) {
            if (arg.contains("spring.batch.job.name") || arg.contains("job.name") || arg.contains("job.names")) {
                return true;
            }
        }
        return false;
    }
}
