package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.MasterDataValidityStatisticsPort;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 기준정보 유효성 통계를 DB 집계 쿼리로 계산하는 출력 어댑터입니다.
 */
@Component
@RequiredArgsConstructor
public class JpaMasterDataValidityStatisticsAdapter implements MasterDataValidityStatisticsPort {

    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final BusinessPartnerRepository businessPartnerRepository;

    @Override
    public long countActiveAccountSubjects(LocalDate asOfDate) {
        return accountSubjectRepository.countActiveAt(asOfDate);
    }

    @Override
    public long countActiveDepartments(LocalDate asOfDate) {
        return departmentRepository.countActiveAt(asOfDate);
    }

    @Override
    public long countActiveProducts(LocalDate asOfDate) {
        return productRepository.countActiveAt(asOfDate);
    }

    @Override
    public long countActiveBusinessPartners(LocalDate asOfDate) {
        return businessPartnerRepository.countActiveAt(asOfDate);
    }
}