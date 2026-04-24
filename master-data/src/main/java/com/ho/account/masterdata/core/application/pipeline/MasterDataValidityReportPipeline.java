package com.ho.account.masterdata.core.application.pipeline;

import com.ho.account.masterdata.batch.application.MasterDataBatchReport;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 마스터 데이터 유효성 보고서 파이프라인
 */
@Service
@RequiredArgsConstructor
public class MasterDataValidityReportPipeline {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final ProductPersistencePort productPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public MasterDataBatchReport createDailyValidityReport(LocalDate asOfDate) {
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now();

        long activeAccountSubjects = accountSubjectPersistencePort.findAll().stream()
                .filter(subject -> MasterDataValidityPolicy.isActiveAt(
                        reportDate, subject.getValidFrom(), subject.getValidTo()))
                .count();

        long activeDepartments = departmentPersistencePort.findAll().stream()
                .filter(department -> MasterDataValidityPolicy.isActiveAt(
                        reportDate, department.getValidFrom(), department.getValidTo()))
                .count();

        long activeProducts = productPersistencePort.findAll().stream()
                .filter(product -> MasterDataValidityPolicy.isActiveAt(
                        reportDate, product.getValidFrom(), product.getValidTo()))
                .count();

        long activeBusinessPartners = businessPartnerPersistencePort.findAll().stream()
                .filter(partner -> Boolean.TRUE.equals(partner.getUseYn()))
                .filter(partner -> MasterDataValidityPolicy.isActiveAt(
                        reportDate, partner.getValidFrom(), partner.getValidTo()))
                .count();

        return new MasterDataBatchReport(
                reportDate,
                activeAccountSubjects,
                activeDepartments,
                activeProducts,
                activeBusinessPartners);
    }
}
