package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;

import java.util.List;

/**
 * 전기 결과로 생성된 GL/SL 엔트리 저장 출력 포트입니다.
 */
public interface LedgerEntryPersistencePort {

    void saveGlEntries(List<GlEntry> entries);

    void saveSlEntries(List<SlEntry> entries);
}
