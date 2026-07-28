package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.MasterDataValidityStatisticsPort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.profiles.active=local",
        "spring.cloud.config.enabled=false",
        "spring.cloud.vault.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaMasterDataValidityStatisticsAdapter.class)
class JpaMasterDataValidityStatisticsAdapterTest {

    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 1);

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private MasterDataValidityStatisticsPort statisticsPort;

    @Test
    void countsOnlyRowsActiveOnRequestedBusinessDate() {
        accountSubjectRepository.save(accountSubject("1000", "현금",
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31)));
        accountSubjectRepository.save(accountSubject("9999", "종료 계정",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        departmentRepository.save(department("D-001", "재무팀"));
        productRepository.save(product("LOAN-001", "대출 상품"));
        businessPartnerRepository.save(businessPartner("BP-001", "거래처"));

        assertThat(statisticsPort.countActiveAccountSubjects(AS_OF_DATE)).isEqualTo(1);
        assertThat(statisticsPort.countActiveDepartments(AS_OF_DATE)).isEqualTo(1);
        assertThat(statisticsPort.countActiveProducts(AS_OF_DATE)).isEqualTo(1);
        assertThat(statisticsPort.countActiveBusinessPartners(AS_OF_DATE)).isEqualTo(1);
    }

    private AccountSubject accountSubject(
            String code,
            String name,
            LocalDate validFrom,
            LocalDate validTo) {
        AccountSubject subject = new AccountSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setCategory(AccountSubject.AccountCategory.ASSETS);
        subject.setBalanceType(AccountSubject.BalanceType.DEBIT);
        subject.setValidFrom(validFrom);
        subject.setValidTo(validTo);
        return subject;
    }

    private Department department(String code, String name) {
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        department.setValidFrom(LocalDate.of(2026, 1, 1));
        department.setValidTo(LocalDate.of(9999, 12, 31));
        return department;
    }

    private Product product(String code, String name) {
        Product product = new Product();
        product.setProductCode(code);
        product.setName(name);
        product.setPrice(BigDecimal.ZERO);
        product.setProductType(Product.ProductType.LOAN);
        product.setValidFrom(LocalDate.of(2026, 1, 1));
        product.setValidTo(LocalDate.of(9999, 12, 31));
        return product;
    }

    private BusinessPartner businessPartner(String code, String name) {
        BusinessPartner partner = new BusinessPartner();
        partner.setBusinessPartnerCode(code);
        partner.setBusinessPartnerName(name);
        partner.setPartnerType(BusinessPartner.PartnerType.VENDOR);
        partner.setUseYn(true);
        partner.setValidFrom(LocalDate.of(2026, 1, 1));
        partner.setValidTo(LocalDate.of(9999, 12, 31));
        return partner;
    }
}