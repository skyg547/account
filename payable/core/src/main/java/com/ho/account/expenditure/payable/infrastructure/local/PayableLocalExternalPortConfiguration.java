package com.ho.account.expenditure.payable.infrastructure.local;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * payable 로컬 실행용 외부 포트 어댑터.
 * 실제 운영에서는 master-data와 journal-ledger 어댑터가 이 Bean을 대체한다.
 */
@Configuration
@Profile("local")
public class PayableLocalExternalPortConfiguration {

    private final AtomicLong journalSequence = new AtomicLong(1L);

    @Bean
    @ConditionalOnMissingBean
    MasterDataQueryPort payableLocalMasterDataQueryPort() {
        return new MasterDataQueryPort() {
            @Override
            public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
                return Optional.of(new AccountSubjectRef(accountCode, "Local account " + accountCode, false, false));
            }

            @Override
            public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
                return Optional.of(new BusinessPartnerRef(businessPartnerCode, "Local vendor " + businessPartnerCode, "VENDOR", true));
            }

            @Override
            public Optional<DepartmentRef> findDepartment(String departmentCode) {
                return Optional.of(new DepartmentRef(departmentCode, "Local department " + departmentCode, "COST_CENTER"));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    JournalPostingPort payableLocalJournalPostingPort() {
        return command -> {
            long id = journalSequence.getAndIncrement();
            return new JournalPostingResult(id, "LOCAL-AP-" + id, "DRAFT");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    AccountingPeriodStatusPort payableLocalAccountingPeriodStatusPort() {
        return date -> false;
    }
}
