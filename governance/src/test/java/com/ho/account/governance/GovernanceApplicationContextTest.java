package com.ho.account.governance;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.audit.application.port.in.MasterApprovalUseCase;
import com.ho.account.audit.web.AuditController;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실행 클래스가 실제 audit/master-data 기능까지 스캔하는지 검증합니다.
 *
 * <p>초보자 설명: 서버 프로세스가 떴다는 사실만으로 API가 등록됐다고 볼 수 없습니다.
 * 핵심 인바운드 포트와 Controller를 주입해 "빈 Spring Boot 서버" 회귀를 막습니다.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class GovernanceApplicationContextTest {

    @Autowired
    private AuditController auditController;

    @Autowired
    private AuditLogUseCase auditLogUseCase;

    @Autowired
    private MasterApprovalUseCase masterApprovalUseCase;

    @Autowired
    private MasterDataChangeRequestUseCase masterDataChangeRequestUseCase;

    @Test
    void loadsGovernanceAndMasterDataApprovalBeans() {
        assertThat(auditController).isNotNull();
        assertThat(auditLogUseCase).isNotNull();
        assertThat(masterApprovalUseCase).isNotNull();
        assertThat(masterDataChangeRequestUseCase).isNotNull();
    }
}
