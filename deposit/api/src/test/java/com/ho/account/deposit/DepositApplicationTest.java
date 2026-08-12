package com.ho.account.deposit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.deposit.adapter.in.web.DepositController;
import com.ho.account.deposit.application.port.in.DepositQueryUseCase;
import com.ho.account.deposit.application.port.in.DepositTransactionUseCase;
import com.ho.account.deposit.application.port.in.DepositUseCase;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * [예금 API 마이크로서비스 ApplicationContext 로딩 통합 테스트]
 *
 * 🐣 [초보자를 위한 설명]
 * 이 테스트는 local 프로파일 활성화 시 H2 인메모리 DB 및 로컬 어댑터가 Spring Container에 정상 등록되어
 * ApplicationContext 로딩 실패 없이 DepositController 및 유즈케이스 포트 빈들이 주입되는지 검증합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class DepositApplicationTest {

    @Autowired
    private DepositController depositController;

    @Autowired
    private OpenAccountUseCase openAccountUseCase;

    @Autowired
    private DepositTransactionUseCase depositTransactionUseCase;

    @Autowired
    private DepositQueryUseCase depositQueryUseCase;

    @Autowired
    private DepositUseCase depositUseCase;

    @Test
    @DisplayName("local 프로파일 구동 시 ApplicationContext 및 컨트롤러/유즈케이스 빈 주입 정상 검증")
    void contextLoadsAndBeansInjectedSuccessfully() {
        assertThat(depositController).isNotNull();
        assertThat(openAccountUseCase).isNotNull();
        assertThat(depositTransactionUseCase).isNotNull();
        assertThat(depositQueryUseCase).isNotNull();
        assertThat(depositUseCase).isNotNull();
    }
}
