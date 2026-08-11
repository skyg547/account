package com.ho.account.deposit.application.port.in;

import java.math.BigDecimal;

/**
 * [헥사고날 아키텍처 - 예금 입출금 인바운드 포트 (Inbound Port)]
 *
 * 🐣 [초보자를 위한 설명]
 * 외부(컨트롤러, 스케줄러, 타 모듈)에서 예금 계좌에 입금(deposit)하거나 출금(withdraw)할 때 호출하는 유즈케이스 인터페이스입니다.
 * 이 포트는 서비스 계층에서 구현하며, 동시성 처리(낙관적 잠금 및 재시도 메커니즘)를 보장하는 엔트리 포인트를 제공합니다.
 */
public interface DepositTransactionUseCase {

    /**
     * 지정한 예금 계좌에 금액을 입금합니다.
     * 동시성 충돌 발생 시 내부적으로 낙관적 잠금 재시도(Retry) 메커니즘을 적용하여 갱신 손실을 방지합니다.
     *
     * @param accountNumber 계좌 번호
     * @param amount 입금 금액
     */
    void deposit(String accountNumber, BigDecimal amount);

    /**
     * 지정한 예금 계좌에서 금액을 출금합니다.
     * 동시성 충돌 발생 시 내부적으로 낙관적 잠금 재시도(Retry) 메커니즘을 적용하여 갱신 손실을 방지합니다.
     *
     * @param accountNumber 계좌 번호
     * @param amount 출금 금액
     */
    void withdraw(String accountNumber, BigDecimal amount);
}
