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
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    AccountSubjectPersistencePort expenditureLocalAccountSubjectPersistencePort() {
        return new AccountSubjectPersistencePort() {
            @Override
            public boolean existsByCode(String code) {
                return true;
            }

            @Override
            public Optional<AccountSubject> findByCode(String code) {
                return Optional.of(localAccountSubject(code));
            }

            @Override
            public List<AccountSubject> findAll() {
                return List.of(localAccountSubject("LOCAL-EXP"));
            }

            @Override
            public AccountSubject save(AccountSubject accountSubject) {
                return accountSubject;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    DepartmentPersistencePort expenditureLocalDepartmentPersistencePort() {
        return new DepartmentPersistencePort() {
            @Override
            public boolean existsByCode(String code) {
                return true;
            }

            @Override
            public Optional<Department> findById(Long id) {
                return Optional.of(localDepartment("LOCAL-DEPT"));
            }

            @Override
            public Optional<Department> findActiveByCode(String code) {
                return Optional.of(localDepartment(code));
            }

            @Override
            public List<Department> findAll() {
                return List.of(localDepartment("LOCAL-DEPT"));
            }

            @Override
            public List<Department> findAllActive() {
                return findAll();
            }

            @Override
            public Department save(Department department) {
                return department;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    BusinessPartnerPersistencePort expenditureLocalBusinessPartnerPersistencePort() {
        return new BusinessPartnerPersistencePort() {
            @Override
            public boolean existsByBusinessPartnerCode(String businessPartnerCode) {
                return true;
            }

            @Override
            public Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode) {
                return Optional.of(localBusinessPartner(businessPartnerCode));
            }

            @Override
            public Optional<BusinessPartner> findById(Long id) {
                return Optional.of(localBusinessPartner("LOCAL-BP"));
            }

            @Override
            public List<BusinessPartner> findAll() {
                return List.of(localBusinessPartner("LOCAL-BP"));
            }

            @Override
            public List<BusinessPartner> findByUseYnTrue() {
                return findAll();
            }

            @Override
            public List<BusinessPartner> findByBusinessPartnerNameContaining(String name) {
                return findAll();
            }

            @Override
            public BusinessPartner save(BusinessPartner businessPartner) {
                return businessPartner;
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

    private AccountSubject localAccountSubject(String code) {
        AccountSubject subject = new AccountSubject();
        subject.setCode(code);
        subject.setName("Local account " + code);
        subject.setBalanceType(AccountSubject.BalanceType.DEBIT);
        subject.setUnsettled(false);
        subject.setFixedAsset(false);
        subject.setValidFrom(LocalDate.of(2020, 1, 1));
        subject.setValidTo(LocalDate.of(9999, 12, 31));
        subject.setAuditUser("LOCAL");
        return subject;
    }

    private Department localDepartment(String code) {
        Department department = new Department();
        department.setCode(code);
        department.setName("Local department " + code);
        department.setType(Department.DepartmentType.COST_CENTER);
        department.setValidFrom(LocalDate.of(2020, 1, 1));
        department.setValidTo(LocalDate.of(9999, 12, 31));
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        department.setAuditUser("LOCAL");
        return department;
    }

    private BusinessPartner localBusinessPartner(String code) {
        BusinessPartner partner = new BusinessPartner();
        partner.setBusinessPartnerCode(code);
        partner.setBusinessPartnerName("Local partner " + code);
        partner.setPartnerType(BusinessPartner.PartnerType.VENDOR);
        partner.setUseYn(true);
        partner.setKycStatus(BusinessPartner.KycStatus.APPROVED);
        partner.setRiskRating(BusinessPartner.RiskRating.LOW);
        partner.setValidFrom(LocalDate.of(2020, 1, 1));
        partner.setValidTo(LocalDate.of(9999, 12, 31));
        partner.setAuditUser("LOCAL");
        return partner;
    }
}
