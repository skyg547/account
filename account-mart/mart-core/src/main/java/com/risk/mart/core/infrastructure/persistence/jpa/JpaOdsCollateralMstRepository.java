package com.risk.mart.core.infrastructure.persistence.jpa;

import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsCollateralMstEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [Infrastructure] 담보 마스터 JPA 리포지토리
 */
@Repository
public interface JpaOdsCollateralMstRepository extends JpaRepository<OdsCollateralMstEntity, String> {
    String QUERY_FOR_DQ_CHECK = """
            SELECT new com.risk.mart.core.domain.ods.loan.OdsCollateralMst(
                c.collateralNo,
                c.customerCode,
                c.collateralType,
                c.currency,
                c.appraisalAmount,
                c.pledgeAmount,
                c.appraisalDate,
                c.isActive)
            FROM OdsCollateralMstEntity c
            ORDER BY c.collateralNo
            """;

    Optional<OdsCollateralMstEntity> findFirstByCustomerCode(String customerCode);
}
