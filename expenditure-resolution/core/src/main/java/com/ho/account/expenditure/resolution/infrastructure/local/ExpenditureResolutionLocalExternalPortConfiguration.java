package com.ho.account.expenditure.resolution.infrastructure.local;

import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * expenditure-resolution 로컬 실행용 외부 포트 어댑터.
 * 지출결의 core 흐름을 H2에서 확인하기 위한 최소 master/journal/tax/asset 경계를 제공한다.
 */
@Configuration
@Profile("local")
public class ExpenditureResolutionLocalExternalPortConfiguration {

    private final AtomicLong journalSequence = new AtomicLong(1L);

    @Bean
    @ConditionalOnMissingBean
    MasterDataQueryPort expenditureLocalMasterDataQueryPort() {
        return new MasterDataQueryPort() {
            @Override
            public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
                return Optional.of(new AccountSubjectRef(accountCode, "Local account " + accountCode, false, false));
            }

            @Override
            public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
                return Optional.of(new BusinessPartnerRef(businessPartnerCode, "Local partner " + businessPartnerCode, "VENDOR", true));
            }

            @Override
            public Optional<DepartmentRef> findDepartment(String departmentCode) {
                return Optional.of(new DepartmentRef(departmentCode, "Local department " + departmentCode, "COST_CENTER"));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    TaxInvoiceQueryPort expenditureLocalTaxInvoiceQueryPort() {
        return taxInvoiceId -> Optional.of(new TaxInvoiceRef(taxInvoiceId, "LOCAL-TAX-" + taxInvoiceId, "PURCHASE", "ACTIVE"));
    }

    @Bean
    @ConditionalOnMissingBean
    AssetRegistrationPort expenditureLocalAssetRegistrationPort() {
        return new AssetRegistrationPort() {
            @Override
            public void registerAcquiredAsset(AssetAcquisitionCommand command) {
                // 로컬 실행에서는 외부 자산 모듈 호출을 기록하지 않고 지출 흐름만 검증한다.
            }

            @Override
            public void activateLeaseContract(Long leaseContractId) {
                // 로컬 실행에서는 리스 활성화 외부 호출을 생략한다.
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    JournalUseCase expenditureLocalJournalUseCase() {
        return new JournalUseCase() {
            @Override
            public JournalEntry createJournalEntry(JournalEntry journalEntry) {
                long id = journalSequence.getAndIncrement();
                journalEntry.setId(id);
                journalEntry.setSlipNo("LOCAL-EXP-" + id);
                journalEntry.setStatus(JournalEntryStatus.DRAFT);
                return journalEntry;
            }

            @Override
            public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate) {
                JournalEntry entry = new JournalEntry();
                entry.setAccountingDate(accountingDate);
                return Optional.of(createJournalEntry(entry));
            }

            @Override
            public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
                return List.of();
            }

            @Override
            public List<JournalEntry> getJournalEntriesBySource(String sourceType, String sourceId) {
                return List.of();
            }

            @Override
            public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
                return Optional.empty();
            }

            @Override
            public Optional<JournalEntry> getJournalEntryWithDetails(Long id) {
                return Optional.empty();
            }

            @Override
            public Optional<JournalEntry> getJournalEntry(Long id) {
                return Optional.empty();
            }

            @Override
            public void approveJournalEntry(Long id, String approver) {
                // 로컬 실행에서는 전표 승인 외부 상태를 저장하지 않는다.
            }

            @Override
            public void postJournalEntry(Long id, String poster) {
                // 로컬 실행에서는 원장 반영을 생략한다.
            }

            @Override
            public JournalEntry reverseJournalEntry(Long id, LocalDate accountingDate, String creator, String reason) {
                JournalEntry reversal = new JournalEntry();
                reversal.setAccountingDate(accountingDate);
                return createJournalEntry(reversal);
            }
        };
    }
}