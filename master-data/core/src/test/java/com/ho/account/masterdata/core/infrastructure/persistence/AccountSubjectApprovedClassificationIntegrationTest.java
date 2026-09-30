package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.service.AccountSubjectMasterDataChangeApplier;
import com.ho.account.masterdata.core.application.service.AccountSubjectService;
import com.ho.account.masterdata.core.application.service.MasterDataChangeRequestService;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountType;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.AccountSubjectMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.MasterDataChangeRequestMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = AccountSubjectApprovedClassificationIntegrationTest.TestApplication.class)
@Import({JpaAccountSubjectPersistenceAdapter.class, AccountSubjectMapper.class, AccountSubjectService.class,
        AccountSubjectMasterDataChangeApplier.class, MasterDataChangeRequestService.class,
        JpaMasterDataChangeRequestPersistenceAdapter.class, JpaMasterDataVersionQueryAdapter.class,
        MasterDataChangeRequestMapper.class, JacksonMasterDataChangePayloadDecoder.class,
        AccountSubjectApprovedClassificationIntegrationTest.JacksonConfig.class})
class AccountSubjectApprovedClassificationIntegrationTest {

    private static final String CODE = "AC-755-APPROVED";
    private static final String MAPPING = "REG-755";
    private static final String REQUESTER = "requester-755";
    private static final String APPROVER = "approver-755";
    private static final String REASON = "Correct display name";

    @Autowired private JpaAccountSubjectPersistenceAdapter accounts;
    @Autowired private JpaMasterDataChangeRequestPersistenceAdapter requests;
    @Autowired private MasterDataChangeRequestService changes;
    @Autowired private AccountSubjectRepository accountRepository;
    @Autowired private TestEntityManager entityManager;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {
    }

    @TestConfiguration
    static class JacksonConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void approvedNameOnlyUpdatePersistsAuditAndBothSCD2Versions(AccountType accountType) {
        LocalDate effectiveDate = LocalDate.now();
        LocalDate originalFrom = effectiveDate.minusYears(1);
        accounts.save(original(accountType, originalFrom));
        entityManager.flush();
        entityManager.clear();

        String payload = "{\"code\":\"" + CODE + "\",\"name\":\"Renamed\",\"validFrom\":\""
                + effectiveDate + "\"}";
        MasterDataChangeRequest requested = changes.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.ACCOUNT_SUBJECT, CODE, ChangeType.UPDATE, effectiveDate, 2,
                REQUESTER, REASON, payload));
        Long requestId = requested.getId();
        assertThat(requestId).isNotNull();
        entityManager.flush();
        entityManager.clear();

        MasterDataChangeRequest storedRequested = requests.findById(requestId).orElseThrow();
        assertThat(storedRequested.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(storedRequested.getRequestedBy()).isEqualTo(REQUESTER);
        assertThat(storedRequested.getRequestedAt()).isNotNull();
        assertThat(storedRequested.getApprovedBy()).isNull();
        assertThat(storedRequested.getApprovedAt()).isNull();
        assertThat(storedRequested.getAppliedAt()).isNull();
        assertThat(storedRequested.getPayloadJson()).isEqualTo(payload);

        changes.approve(requestId, APPROVER);
        entityManager.flush();
        entityManager.clear();

        MasterDataChangeRequest storedApproved = requests.findById(requestId).orElseThrow();
        assertThat(storedApproved.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(storedApproved.getApprovedBy()).isEqualTo(APPROVER);
        assertThat(storedApproved.getApprovedAt()).isNotNull();
        assertThat(storedApproved.getAppliedAt()).isNull();
        assertThat(accountRepository.countByCode(CODE)).isEqualTo(1);

        changes.applyApprovedChange(requestId);
        entityManager.flush();
        entityManager.clear();

        AccountSubject historical = accounts.findByCodeAt(CODE, effectiveDate.minusDays(1)).orElseThrow();
        AccountSubject successor = accounts.findByCodeAt(CODE, effectiveDate).orElseThrow();
        assertThat(accountRepository.countByCode(CODE)).isEqualTo(2);
        assertThat(historical.getId()).isNotEqualTo(successor.getId());
        assertThat(historical.getName()).isEqualTo("Original");
        assertThat(historical.getValidFrom()).isEqualTo(originalFrom);
        assertThat(historical.getValidTo()).isEqualTo(effectiveDate.minusDays(1));
        assertThat(historical.getAccountType()).isEqualTo(accountType);
        assertThat(historical.getRegulatoryMappingCode()).isEqualTo(MAPPING);
        assertThat(successor.getName()).isEqualTo("Renamed");
        assertThat(successor.getValidFrom()).isEqualTo(effectiveDate);
        assertThat(successor.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
        assertThat(successor.getCategory()).isEqualTo(historical.getCategory());
        assertThat(successor.getBalanceType()).isEqualTo(historical.getBalanceType());
        assertThat(successor.getAccountType()).isEqualTo(accountType);
        assertThat(successor.getRegulatoryMappingCode()).isEqualTo(MAPPING);
        assertThat(successor.isUnsettled()).isTrue();
        assertThat(successor.isFixedAsset()).isTrue();

        MasterDataChangeRequest storedApplied = requests.findById(requestId).orElseThrow();
        assertThat(storedApplied.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(storedApplied.getChangeType()).isEqualTo(ChangeType.UPDATE);
        assertThat(storedApplied.getTargetType()).isEqualTo(MasterDataType.ACCOUNT_SUBJECT);
        assertThat(storedApplied.getTargetKey()).isEqualTo(CODE);
        assertThat(storedApplied.getEffectiveDate()).isEqualTo(effectiveDate);
        assertThat(storedApplied.getRequestedVersion()).isEqualTo(2);
        assertThat(storedApplied.getRequestedBy()).isEqualTo(REQUESTER);
        assertThat(storedApplied.getApprovedBy()).isEqualTo(APPROVER);
        assertThat(storedApplied.getRequestedAt()).isEqualTo(storedRequested.getRequestedAt());
        assertThat(storedApplied.getApprovedAt()).isEqualTo(storedApproved.getApprovedAt());
        assertThat(storedApplied.getReason()).isEqualTo(REASON);
        assertThat(storedApplied.getPayloadJson()).isEqualTo(payload);
        assertThat(storedApplied.getAppliedAt()).isNotNull();
        assertThat(storedApplied.getAppliedAt()).isAfterOrEqualTo(storedApplied.getApprovedAt());
    }

    private static AccountSubject original(AccountType accountType, LocalDate originalFrom) {
        AccountSubject account = new AccountSubject();
        account.setCode(CODE);
        account.setName("Original");
        account.setCategory(accountType == AccountType.NON_OPERATING_INCOME
                ? AccountSubject.AccountCategory.REVENUE : AccountSubject.AccountCategory.EXPENSES);
        account.setAccountType(accountType);
        account.setBalanceType(accountType == AccountType.NON_OPERATING_INCOME
                ? AccountSubject.BalanceType.CREDIT : AccountSubject.BalanceType.DEBIT);
        account.setRegulatoryMappingCode(MAPPING);
        account.setUnsettled(true);
        account.setFixedAsset(true);
        account.setValidFrom(originalFrom);
        account.setValidTo(LocalDate.of(9999, 12, 31));
        return account;
    }
}
