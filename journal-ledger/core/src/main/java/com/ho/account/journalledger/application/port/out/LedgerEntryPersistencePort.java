package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;

/**
 * 전기 결과 Aggregate를 저장 기술로 전달하는 출력 포트입니다.
 *
 * <p>포트가 JPA 엔티티 목록을 노출하면 application service가 DB 저장 모양을 알아야 합니다.
 * 대신 하나의 {@link GeneralLedger}를 전달해 GL/SL 양쪽이 같은 불변 posting snapshot을
 * 저장하도록 보장합니다.</p>
 */
public interface LedgerEntryPersistencePort {

    void save(GeneralLedger generalLedger);
}
