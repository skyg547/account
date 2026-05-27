package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 여신 도메인 원천 계정 원장 데이터소스 인터페이스.
 * 💡 [헥사고날 아키텍처] 특정 영속성 기술(JPA)로부터 비즈니스 로직을 격리하는 순수 포트입니다.
 */
public interface OdsAccountLedgerRepository {
    Optional<OdsAccountLedger> findById(String accountNo);
    List<OdsAccountLedger> findAll();
    OdsAccountLedger save(OdsAccountLedger ledger);
    void saveAll(List<OdsAccountLedger> ledgers);
    long count();
}
