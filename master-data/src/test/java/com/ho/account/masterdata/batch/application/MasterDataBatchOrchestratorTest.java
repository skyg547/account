package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.port.out.ProductPersistencePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataBatchOrchestratorTest {

    @Test
    void createsDailyValidityReportFromPorts() {
        LocalDate asOfDate = LocalDate.of(2026, 4, 20);
        MasterDataBatchOrchestrator orchestrator = new MasterDataBatchOrchestrator(
                new FakeAccountSubjectPort(),
                new FakeDepartmentPort(),
                new FakeProductPort(),
                new FakeBusinessPartnerPort());

        MasterDataBatchReport report = orchestrator.createDailyValidityReport(asOfDate);

        assertThat(report.asOfDate()).isEqualTo(asOfDate);
        assertThat(report.activeAccountSubjects()).isEqualTo(1);
        assertThat(report.activeDepartments()).isEqualTo(1);
        assertThat(report.activeProducts()).isEqualTo(1);
        assertThat(report.activeBusinessPartners()).isEqualTo(1);
    }

    private static final class FakeAccountSubjectPort implements AccountSubjectPersistencePort {
        @Override
        public boolean existsByCode(String code) {
            return false;
        }

        @Override
        public Optional<AccountSubject> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<AccountSubject> findAll() {
            AccountSubject active = new AccountSubject();
            active.setCode("1000");
            active.setValidFrom(LocalDate.of(2026, 4, 1));
            active.setValidTo(LocalDate.of(2026, 4, 30));
            return List.of(active);
        }

        @Override
        public AccountSubject save(AccountSubject accountSubject) {
            return accountSubject;
        }
    }

    private static final class FakeDepartmentPort implements DepartmentPersistencePort {
        @Override
        public boolean existsByCode(String code) {
            return false;
        }

        @Override
        public Optional<Department> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<Department> findAll() {
            Department active = new Department();
            active.setCode("HR");
            active.setValidFrom(LocalDate.of(2026, 1, 1));
            active.setValidTo(LocalDate.of(2026, 12, 31));
            return List.of(active);
        }

        @Override
        public Department save(Department department) {
            return department;
        }
    }

    private static final class FakeProductPort implements ProductPersistencePort {
        @Override
        public boolean existsByProductCode(String productCode) {
            return false;
        }

        @Override
        public Optional<Product> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<Product> findByProductCode(String productCode) {
            return Optional.empty();
        }

        @Override
        public List<Product> findAll() {
            Product active = new Product();
            active.setProductCode("P-1");
            active.setValidFrom(LocalDate.of(2026, 4, 1));
            active.setValidTo(LocalDate.of(2026, 5, 1));
            return List.of(active);
        }

        @Override
        public Product save(Product product) {
            return product;
        }
    }

    private static final class FakeBusinessPartnerPort implements BusinessPartnerPersistencePort {
        @Override
        public boolean existsByBusinessPartnerCode(String businessPartnerCode) {
            return false;
        }

        @Override
        public Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode) {
            return Optional.empty();
        }

        @Override
        public Optional<BusinessPartner> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public List<BusinessPartner> findAll() {
            return findByUseYnTrue();
        }

        @Override
        public List<BusinessPartner> findByUseYnTrue() {
            BusinessPartner partner = new BusinessPartner();
            partner.setBusinessPartnerCode("BP-1");
            partner.setUseYn(true);
            return List.of(partner);
        }

        @Override
        public List<BusinessPartner> findByBusinessPartnerNameContaining(String name) {
            return List.of();
        }

        @Override
        public BusinessPartner save(BusinessPartner businessPartner) {
            return businessPartner;
        }
    }
}
