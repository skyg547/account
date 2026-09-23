package com.ho.account.masterdata.api;

import static com.ho.account.masterdata.core.domain.model.AccountSubject.AccountCategory.ASSETS;
import static com.ho.account.masterdata.core.domain.model.AccountSubject.AccountCategory.EQUITY;
import static com.ho.account.masterdata.core.domain.model.AccountSubject.AccountCategory.EXPENSES;
import static com.ho.account.masterdata.core.domain.model.AccountSubject.AccountCategory.LIABILITIES;
import static com.ho.account.masterdata.core.domain.model.AccountSubject.AccountCategory.REVENUE;
import static com.ho.account.masterdata.core.domain.model.AccountSubject.BalanceType.CREDIT;
import static com.ho.account.masterdata.core.domain.model.AccountSubject.BalanceType.DEBIT;
import static com.ho.account.masterdata.core.domain.model.Department.DepartmentType.COST_CENTER;
import static com.ho.account.masterdata.core.domain.model.Department.DepartmentType.PROFIT_CENTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.ho.account.masterdata.MasterDataApplication;
import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountType;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.CurrencyRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(
        classes = MasterDataApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:master-data-local-seed-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.cloud.config.import-check.enabled=false"
        })
class MasterDataLocalProfileDataSeedTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2020, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(9999, 12, 31);

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Test
    void localProfileSeedsCurrencies() {
        assertThat(currencyRepository.findAll())
                .hasSize(3)
                .extracting("currencyCode", "currencyName", "validFrom", "validTo")
                .containsExactlyInAnyOrder(
                        tuple("KRW", "원화", VALID_FROM, VALID_TO),
                        tuple("USD", "달러", VALID_FROM, VALID_TO),
                        tuple("EUR", "유로", VALID_FROM, VALID_TO));

        assertThat(currencyRepository.findActiveByCurrencyCode("KRW", LocalDate.of(2026, 1, 1)))
                .isPresent()
                .get()
                .extracting("currencyName")
                .isEqualTo("원화");
    }

    @Test
    void localProfileSeedsDepartments() {
        assertThat(departmentRepository.findAll())
                .hasSize(3)
                .extracting("code", "name", "type", "validFrom", "validTo")
                .containsExactlyInAnyOrder(
                        tuple("DEPT_FINANCE", "재무팀", COST_CENTER, VALID_FROM, VALID_TO),
                        tuple("DEPT_ACCOUNTING", "회계팀", COST_CENTER, VALID_FROM, VALID_TO),
                        tuple("DEPT_MGMT", "경영관리팀", PROFIT_CENTER, VALID_FROM, VALID_TO));

        assertThat(departmentRepository.findActiveByCode("DEPT_ACCOUNTING", LocalDate.of(2026, 1, 1)))
                .isPresent()
                .get()
                .extracting("name")
                .isEqualTo("회계팀");
    }

    @Test
    void localProfileSeedsAccountSubjects() {
        assertThat(accountSubjectRepository.findAll())
                .hasSize(8)
                .extracting("code", "name", "accountType", "balanceType", "category", "validFrom", "validTo")
                .containsExactlyInAnyOrder(
                        tuple("10100", "현금", AccountType.ASSETS, DEBIT, ASSETS, VALID_FROM, VALID_TO),
                        tuple("10200", "보통예금", AccountType.ASSETS, DEBIT, ASSETS, VALID_FROM, VALID_TO),
                        tuple("20100", "외상매입금", AccountType.LIABILITIES, CREDIT, LIABILITIES, VALID_FROM, VALID_TO),
                        tuple("20200", "미지급금", AccountType.LIABILITIES, CREDIT, LIABILITIES, VALID_FROM, VALID_TO),
                        tuple("30100", "자본금", AccountType.EQUITY, CREDIT, EQUITY, VALID_FROM, VALID_TO),
                        tuple("40100", "매출액", AccountType.REVENUE, CREDIT, REVENUE, VALID_FROM, VALID_TO),
                        tuple("50100", "급여", AccountType.EXPENSES, DEBIT, EXPENSES, VALID_FROM, VALID_TO),
                        tuple("50200", "복리후생비", AccountType.EXPENSES, DEBIT, EXPENSES, VALID_FROM, VALID_TO));

        assertThat(accountSubjectRepository.findActiveByCode("50100", LocalDate.of(2026, 1, 1)))
                .isPresent()
                .get()
                .extracting("name")
                .isEqualTo("급여");
    }
}
