package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 각 기준정보 테이블의 업무 키별 SCD2 행 개수를 버전 순서로 변환하는 JPA 어댑터입니다.
 */
@Component
@RequiredArgsConstructor
public class JpaMasterDataVersionQueryAdapter implements MasterDataVersionQueryPort {

    private final AccountSubjectRepository accountSubjectRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;

    @Override
    public long countPersistedVersions(MasterDataType targetType, String targetKey) {
        return switch (targetType) {
            case ACCOUNT_SUBJECT -> accountSubjectRepository.countByCode(targetKey);
            case BUSINESS_PARTNER -> businessPartnerRepository.countByBusinessPartnerCode(targetKey);
            case DEPARTMENT -> departmentRepository.countByCode(targetKey);
            case PRODUCT -> productRepository.countByProductCode(targetKey);
            case CURRENCY, EXCHANGE_RATE, FISCAL_PERIOD -> throw new IllegalStateException(
                    "Version query is not available for unsupported targetType: " + targetType);
        };
    }
}