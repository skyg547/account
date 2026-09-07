package com.ho.account.receivable.infrastructure.local;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * receivable 로컬 실행용 외부 포트 어댑터.
 * 고객/계정/전표 시스템이 없더라도 H2 기반 API와 배치 컨텍스트를 올릴 수 있게 한다.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(
        prefix = "receivable.remote",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class ReceivableLocalExternalPortConfiguration {

    private final AtomicLong journalSequence = new AtomicLong(1L);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "receivable.master-data.remote",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true)
    MasterDataQueryPort receivableLocalMasterDataQueryPort() {
        return new MasterDataQueryPort() {
            @Override
            public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
                return Optional.of(new AccountSubjectRef(accountCode, "Local account " + accountCode, false, false));
            }

            @Override
            public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
                return Optional.of(new BusinessPartnerRef(businessPartnerCode, "Local customer " + businessPartnerCode, "CUSTOMER", true));
            }

            @Override
            public Optional<DepartmentRef> findDepartment(String departmentCode) {
                return Optional.of(new DepartmentRef(departmentCode, "Local department " + departmentCode, "COST_CENTER"));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "receivable.journal-ledger.remote",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true)
    JournalPostingPort receivableLocalJournalPostingPort() {
        return new JournalPostingPort() {
            @Override
            public JournalPostingResult createDraftEntry(com.ho.account.contracts.journal.JournalEntryCommand command) {
                long id = journalSequence.getAndIncrement();
                return new JournalPostingResult(id, "LOCAL-AR-" + id, "DRAFT");
            }

            @Override
            public void approveAndPost(Long journalEntryId, String actor) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "receivable.master-data.remote",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true)
    AccountingPeriodStatusPort receivableLocalAccountingPeriodStatusPort() {
        return date -> false;
    }
}
