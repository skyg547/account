package com.ho.account.deposit.infrastructure.config;

import com.ho.account.deposit.domain.DepositAccountStateMachine;
import com.ho.account.deposit.service.DepositInterestAccrualCalculator;
import com.ho.account.deposit.service.DepositTerminationSettlementCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [도메인 서비스 스프링 빈 등록 설정 (Domain Configuration)]
 *
 * 💡 [설계 배경 및 헥사고날 아키텍처 원칙]
 * Pure Domain 법칙에 따라 domain 및 service 패키지의 도메인 서비스 클래스들
 * (`DepositAccountStateMachine`, `DepositInterestAccrualCalculator`, `DepositTerminationSettlementCalculator` 등)에는
 * Spring의 `@Component` / `@Service` 어노테이션이 일절 포함되지 않습니다.
 *
 * 이 설정 클래스는 인프라(Infrastructure) 계층에 위치하여 순수한 POJO 도메인 객체들을
 * Spring Container의 Bean으로 명시적으로 수동 등록(Explicit Bean Configuration)합니다.
 * 이를 통해 도메인 모델의 순수성을 지키면서도 애플리케이션 계층에서 의존성 주입(DI)을 활용할 수 있습니다.
 */
@Configuration
public class DepositDomainConfiguration {

    @Bean
    public DepositAccountStateMachine depositAccountStateMachine() {
        return new DepositAccountStateMachine();
    }

    @Bean
    public DepositInterestAccrualCalculator depositInterestAccrualCalculator() {
        return new DepositInterestAccrualCalculator();
    }

    @Bean
    public DepositTerminationSettlementCalculator depositTerminationSettlementCalculator(
            DepositInterestAccrualCalculator depositInterestAccrualCalculator) {
        return new DepositTerminationSettlementCalculator(depositInterestAccrualCalculator);
    }
}
