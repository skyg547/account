package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.AllowanceInputPositionSnapshot;
import com.ho.account.ecl.core.application.port.out.AllowanceInputPositionRepository;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaAllowanceInputPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Adapter] AllowanceInputPositionRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class AllowanceInputPositionPersistenceAdapter implements AllowanceInputPositionRepository {

    private final JpaAllowanceInputPositionRepository jpaRepository;

    @Override
    public List<AllowanceInputPositionSnapshot> findByBaseDt(LocalDate baseDt) {
        return jpaRepository.findByBaseDt(baseDt).stream()
                .map(entity -> new AllowanceInputPositionSnapshot(
                        entity.getBaseDt(), entity.getAccNo(), entity.getCustomerCode(), entity.getCustomerName(),
                        entity.getCustomerType(), entity.getIsSme(), entity.getCountryCode(), entity.getProductCode(),
                        entity.getProductCategory(), entity.getCurrency(), entity.getOutstandingAmount(),
                        entity.getLimitAmount(), entity.getInterestRate(), entity.getOpenDate(), entity.getMaturityDate(),
                        entity.getRepaymentMethod(), entity.getGracePeriod(), entity.getRepaymentFreq(),
                        entity.getInternalRating(), entity.getIndustryCode(), entity.getWarningLevel(),
                        entity.getIsDebtRestructured(), entity.getDelinquentDays(), entity.getStaging(),
                        entity.getBranchCode(), entity.getBusinessUnitCode()))
                .toList();
    }
}

