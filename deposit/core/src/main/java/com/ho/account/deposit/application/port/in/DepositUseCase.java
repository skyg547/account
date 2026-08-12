package com.ho.account.deposit.application.port.in;

/**
 * [헥사고날 아키텍처 - 통합 예금 인바운드 포트 (Composite Inbound Port)]
 *
 * 🐣 [초보자를 위한 설명]
 * 예금 계좌 개설(OpenAccountUseCase), 입출금 거래(DepositTransactionUseCase), 계좌 조회(DepositQueryUseCase) 등
 * 예금 도메인 관련 유즈케이스 포트들을 하나로 묶어 제공하는 컴포지트(Composite) 인터페이스입니다.
 * 클라이언트가 개별 포트를 개별적으로 주입받거나, 통합된 단일 인터페이스 포트를 통해 편리하게 의존성을 주입받을 수 있도록 지원합니다.
 */
public interface DepositUseCase extends OpenAccountUseCase, DepositTransactionUseCase, DepositQueryUseCase {
}
