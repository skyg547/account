package com.ho.account.expenditure.resolution.infrastructure.local;

import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
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
    JournalPostingPort expenditureLocalJournalPostingPort() {
        return command -> {
            long id = journalSequence.getAndIncrement();
            return new JournalPostingResult(id, "LOCAL-EXP-" + id, "DRAFT");
        };
    }
}
