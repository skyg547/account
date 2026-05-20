package com.ho.account.deposit.application.port.out;

import com.ho.account.deposit.domain.DepositAccount;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 아웃바운드 포트 (Outbound Port)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 인터페이스는 예금 모듈(애플리케이션)이 데이터베이스(인프라)와 소통하기 위해 뚫어놓은 '창구(Port)'입니다.
 * 비즈니스 로직을 담은 서비스는 JPA나 DB에 대해 전혀 몰라도 이 창구를 통해서 "계좌 저장해줘", "계좌 찾아줘"라고 요청할 수 있습니다.
 * 실제 DB에 어떻게 저장할지(JPA, MyBatis 등)는 이 포트를 구현하는 어댑터(Adapter)가 결정합니다.
 */
public interface DepositAccountPersistencePort {
    DepositAccount save(DepositAccount depositAccount);
    Optional<DepositAccount> findByAccountNumber(String accountNumber);
}
