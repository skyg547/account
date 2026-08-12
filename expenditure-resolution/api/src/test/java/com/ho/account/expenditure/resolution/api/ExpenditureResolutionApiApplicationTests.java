package com.ho.account.expenditure.resolution.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.expenditure.application.port.in.APPaymentUseCase;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.resolution.api.adapter.in.web.APPaymentController;
import com.ho.account.expenditure.resolution.api.adapter.in.web.ExpenditureController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

/**
 * [Expenditure Resolution API ApplicationContext Loading Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent (교육적 설명 및 설계 의도):</strong></p>
 * <ul>
 *   <li><strong>ApplicationContext Loading Verification (애플리케이션 컨텍스트 로딩 검증)</strong>:
 *       Spring Boot API 애플리케이션의 핵심 구성 요소, Web Controller, 서비스 계층, 외부 포트 스텁 및 의존성 주입이
 *       정상 작동하며 Spring Container(ApplicationContext)가 오류 없이 성공적으로 구동되는지 검증합니다.</li>
 *   <li><strong>Profile Isolation (local 프로파일 테스트 격리)</strong>:
 *       {@code @ActiveProfiles("local")}을 활성화하여 테스트 실행 시 외부 PostgreSQL 데이터베이스, Cloud Config, Eureka Discovery,
 *       Vault 등 인프라스트럭처 제어 플레인에 의존하지 않고 H2 인메모리 DB 기반으로 독립 실행(Self-contained Test Environment)될 수 있도록 구성합니다.</li>
 *   <li><strong>Hexagonal Architecture & Port Injection (헥사고날 포트 및 웹 어댑터 빈 검증)</strong>:
 *       Inbound Web Adapter({@link ExpenditureController}, {@link APPaymentController}),
 *       Inbound UseCase({@link ExpenditureResolutionUseCase}, {@link APPaymentUseCase}),
 *       및 Outbound Port Stub({@link MasterDataQueryPort}, {@link TaxInvoiceQueryPort}, {@link JournalPostingPort}, {@link AssetRegistrationPort})이
 *       컨텍스트 내에 올바르게 등록되고 주입되는지 확인합니다.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class ExpenditureResolutionApiApplicationTests {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private ExpenditureController expenditureController;

    @Autowired
    private APPaymentController apPaymentController;

    @Autowired
    private ExpenditureResolutionUseCase expenditureResolutionUseCase;

    @Autowired
    private APPaymentUseCase apPaymentUseCase;

    @Autowired
    private MasterDataQueryPort masterDataQueryPort;

    @Autowired
    private TaxInvoiceQueryPort taxInvoiceQueryPort;

    @Autowired
    private JournalPostingPort journalPostingPort;

    @Autowired
    private AssetRegistrationPort assetRegistrationPort;

    @Test
    @DisplayName("local 프로파일 환경에서 Expenditure Resolution API 애플리케이션의 ApplicationContext가 성공적으로 로드된다")
    void contextLoads() {
        // 1. Context Activation & Active Profile Verification
        assertThat(context.isActive()).isTrue();
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();

        // 2. Web Controller Inbound Adapter Verification
        assertThat(expenditureController).isNotNull();
        assertThat(apPaymentController).isNotNull();

        // 3. Domain UseCase Service Verification
        assertThat(expenditureResolutionUseCase).isNotNull();
        assertThat(apPaymentUseCase).isNotNull();

        // 4. External Outbound Port Stub Verification
        assertThat(masterDataQueryPort).isNotNull();
        assertThat(taxInvoiceQueryPort).isNotNull();
        assertThat(journalPostingPort).isNotNull();
        assertThat(assetRegistrationPort).isNotNull();

        // 5. In-Memory H2 Datasource Configuration Verification
        assertThat(environment.getProperty("spring.datasource.url"))
                .contains("jdbc:h2:mem:")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
    }
}
