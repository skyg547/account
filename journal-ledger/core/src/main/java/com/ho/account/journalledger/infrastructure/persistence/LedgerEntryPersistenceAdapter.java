package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlEntryRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.SlEntryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 전기 결과 Aggregate를 GL/SL JPA 엔티티로 변환해 저장하는 출력 어댑터입니다.
 *
 * <p>도메인 {@link GeneralLedger.Posting}은 DB 매핑을 모르고, 이 클래스만
 * {@link GlEntry}/{@link SlEntry}의 컬럼 구조를 압니다. 따라서 JPA 모델이 바뀌어도
 * application service와 전기 Aggregate는 영향을 받지 않습니다.</p>
 */
@Component
@ConditionalOnProperty(name = "journal-ledger.ledger.persistence-mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class LedgerEntryPersistenceAdapter implements LedgerEntryPersistencePort {

    private final GlEntryRepository glEntryRepository;
    private final SlEntryRepository slEntryRepository;
    private final EntityManager entityManager;

    @Override
    public void save(GeneralLedger generalLedger) {
        if (generalLedger == null) {
            throw new IllegalArgumentException("저장할 GeneralLedger Aggregate는 필수입니다.");
        }

        List<GlEntry> glEntries = new ArrayList<>(generalLedger.postings().size());
        List<SlEntry> slEntries = new ArrayList<>(generalLedger.postings().size());
        for (GeneralLedger.Posting posting : generalLedger.postings()) {
            // 실제 전표 라인을 다시 조회하지 않고 프록시 참조만 얻습니다. Aggregate가 이미
            // persisted detail ID를 검증했기 때문에 추가 SELECT 없이 lineage FK를 연결합니다.
            JournalDetail journalDetail =
                    entityManager.getReference(JournalDetail.class, posting.journalDetailId());
            glEntries.add(toGlEntry(posting, journalDetail));
            slEntries.add(toSlEntry(posting, journalDetail));
        }

        glEntryRepository.saveAll(glEntries);
        slEntryRepository.saveAll(slEntries);
    }

    private GlEntry toGlEntry(GeneralLedger.Posting posting, JournalDetail journalDetail) {
        GlEntry entry = new GlEntry();
        entry.setJournalDetail(journalDetail);
        entry.setAccountCode(posting.accountCode());
        entry.setCurrencyCode(posting.currencyCode());
        entry.setFiscalYear(posting.fiscalYear());
        entry.setFiscalPeriod(posting.fiscalPeriod());
        entry.setPostingDate(posting.postingDate());
        entry.setDrAmount(posting.debit().amount());
        entry.setCrAmount(posting.credit().amount());
        entry.setBaseDrAmount(posting.baseDebit().amount());
        entry.setBaseCrAmount(posting.baseCredit().amount());
        entry.setSummary(posting.summary());
        entry.setLineageSourceType(posting.lineageSourceType());
        entry.setLineageSourceId(posting.lineageSourceId());
        return entry;
    }

    private SlEntry toSlEntry(GeneralLedger.Posting posting, JournalDetail journalDetail) {
        SlEntry entry = new SlEntry();
        entry.setJournalDetail(journalDetail);
        entry.setAccountCode(posting.accountCode());
        entry.setBusinessPartnerCode(posting.businessPartnerCode());
        entry.setDepartmentCode(posting.departmentCode());
        entry.setCurrencyCode(posting.currencyCode());
        entry.setFiscalYear(posting.fiscalYear());
        entry.setFiscalPeriod(posting.fiscalPeriod());
        entry.setPostingDate(posting.postingDate());
        entry.setDrAmount(posting.debit().amount());
        entry.setCrAmount(posting.credit().amount());
        entry.setBaseDrAmount(posting.baseDebit().amount());
        entry.setBaseCrAmount(posting.baseCredit().amount());
        entry.setSummary(posting.summary());
        entry.setLineageSourceType(posting.lineageSourceType());
        entry.setLineageSourceId(posting.lineageSourceId());
        return entry;
    }
}
