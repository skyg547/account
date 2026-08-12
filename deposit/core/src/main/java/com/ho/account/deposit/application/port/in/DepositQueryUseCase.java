package com.ho.account.deposit.application.port.in;

import com.ho.account.deposit.domain.DepositAccount;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 예금 조회 인바운드 포트 (Inbound Query Port)]
 *
 * 🐣 [초보자를 위한 설명]
 * 외부(웹 컨트롤러, 스케줄러, 타 마듈 REST API)에서 예금 계좌 단건 및 상태 조회를 요청할 때 사용하는 쿼리 전용 유즈케이스 포트 인터페이스입니다.
 * CQRS(Command Query Responsibility Segregation) 원칙에 따라 상태를 변경하는 Command 유즈케이스와
 * 단순 상태를 읽어오는 Query 유즈케이스를 분리하여 단일 책임 원칙(SRP)을 준수합니다.
 */
public interface DepositQueryUseCase {

    /**
     * 계좌번호를 기반으로 예금 계좌 상세 정보를 조회합니다.
     *
     * @param accountNumber 조회할 예금 계좌번호
     * @return 조회된 예금 계좌 객체 (존재하지 않을 경우 Optional.empty())
     */
    Optional<DepositAccount> findByAccountNumber(String accountNumber);
}
