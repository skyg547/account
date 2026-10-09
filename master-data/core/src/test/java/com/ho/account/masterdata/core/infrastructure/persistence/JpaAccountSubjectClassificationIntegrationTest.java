package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.service.AccountSubjectService;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountType;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.AccountSubjectMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = JpaAccountSubjectClassificationIntegrationTest.TestApplication.class)
@Import({JpaAccountSubjectPersistenceAdapter.class, AccountSubjectMapper.class, AccountSubjectService.class,
        JpaMasterDataBusinessKeyLockAdapter.class})
class JpaAccountSubjectClassificationIntegrationTest {

    private static final LocalDate OLD_FROM = LocalDate.of(2025, 1, 1);
    private static final LocalDate NEW_FROM = LocalDate.of(2026, 8, 1);

    @Autowired private JpaAccountSubjectPersistenceAdapter adapter;
    @Autowired private AccountSubjectService service;
    @Autowired private AccountSubjectRepository repository;
    @Autowired private TestEntityManager entityManager;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void nameOnlyUpdateKeepsClassificationAcrossFlushClearReload(AccountType accountType) {
        AccountSubject original = original(accountType);
        adapter.save(original);
        entityManager.flush();
        entityManager.clear();

        service.updateAccountSubject("AC-755", new AccountSubjectCommand("AC-755", "Renamed", null,
                null, null, null, null, null, NEW_FROM, null, null, null, false));
        entityManager.flush();
        entityManager.clear();

        List<AccountSubject> versions = repository.findAll().stream()
                .filter(entity -> entity.getCode().equals("AC-755"))
                .map(entity -> adapter.findByCodeAt("AC-755", entity.getValidFrom()).orElseThrow())
                .toList();
        assertThat(versions).hasSize(2);
        AccountSubject historical = versions.stream().filter(version -> version.getValidFrom().equals(OLD_FROM))
                .findFirst().orElseThrow();
        AccountSubject successor = versions.stream().filter(version -> version.getValidFrom().equals(NEW_FROM))
                .findFirst().orElseThrow();
        assertThat(historical.getName()).isEqualTo("Original");
        assertThat(historical.getValidTo()).isEqualTo(NEW_FROM.minusDays(1));
        assertThat(adapter.findByCodeAt("AC-755", NEW_FROM.minusDays(1)).orElseThrow().getId())
                .isEqualTo(historical.getId());
        assertThat(adapter.findByCodeAt("AC-755", NEW_FROM).orElseThrow().getId())
                .isEqualTo(successor.getId());
        assertThat(successor.getId()).isNotEqualTo(historical.getId());
        assertThat(historical.getAccountType()).isEqualTo(accountType);
        assertThat(historical.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(successor.getName()).isEqualTo("Renamed");
        assertThat(successor.getAccountType()).isEqualTo(accountType);
        assertThat(successor.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(successor.isUnsettled()).isTrue();
        assertThat(successor.isFixedAsset()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void explicitMappingClearChangesOnlySuccessor(AccountType accountType) {
        adapter.save(original(accountType));
        entityManager.flush();
        entityManager.clear();

        service.updateAccountSubject("AC-755", new AccountSubjectCommand("AC-755", "Cleared", null,
                null, null, null, null, null, NEW_FROM, null, null, null, true));
        entityManager.flush();
        entityManager.clear();

        AccountSubject historical = adapter.findByCodeAt("AC-755", OLD_FROM).orElseThrow();
        AccountSubject successor = adapter.findByCodeAt("AC-755", NEW_FROM).orElseThrow();
        assertThat(historical.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(historical.getAccountType()).isEqualTo(accountType);
        assertThat(successor.getRegulatoryMappingCode()).isNull();
        assertThat(successor.getAccountType()).isEqualTo(accountType);
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void explicitTypeAndMappingReplacementChangesOnlySuccessor(AccountType originalType) {
        adapter.save(original(originalType));
        entityManager.flush();
        entityManager.clear();
        AccountType replacement = originalType == AccountType.NON_OPERATING_INCOME
                ? AccountType.NON_OPERATING_EXPENSES : AccountType.NON_OPERATING_INCOME;

        service.updateAccountSubject("AC-755", new AccountSubjectCommand("AC-755", "Reclassified", null,
                replacement == AccountType.NON_OPERATING_INCOME
                        ? AccountSubject.AccountCategory.REVENUE : AccountSubject.AccountCategory.EXPENSES,
                replacement == AccountType.NON_OPERATING_INCOME
                        ? AccountSubject.BalanceType.CREDIT : AccountSubject.BalanceType.DEBIT,
                null, null, null, NEW_FROM, null, replacement, "REG-NEW", false));
        entityManager.flush();
        entityManager.clear();

        AccountSubject historical = adapter.findByCodeAt("AC-755", OLD_FROM).orElseThrow();
        AccountSubject successor = adapter.findByCodeAt("AC-755", NEW_FROM).orElseThrow();
        assertThat(historical.getAccountType()).isEqualTo(originalType);
        assertThat(historical.getRegulatoryMappingCode()).isEqualTo("REG-755");
        assertThat(successor.getAccountType()).isEqualTo(replacement);
        assertThat(successor.getRegulatoryMappingCode()).isEqualTo("REG-NEW");
    }

    private static AccountSubject original(AccountType accountType) {
        AccountSubject account = new AccountSubject();
        account.setCode("AC-755");
        account.setName("Original");
        account.setCategory(accountType == AccountType.NON_OPERATING_INCOME
                ? AccountSubject.AccountCategory.REVENUE : AccountSubject.AccountCategory.EXPENSES);
        account.setAccountType(accountType);
        account.setBalanceType(accountType == AccountType.NON_OPERATING_INCOME
                ? AccountSubject.BalanceType.CREDIT : AccountSubject.BalanceType.DEBIT);
        account.setRegulatoryMappingCode("REG-755");
        account.setUnsettled(true);
        account.setFixedAsset(true);
        account.setValidFrom(OLD_FROM);
        account.setValidTo(LocalDate.of(9999, 12, 31));
        return account;
    }
}
