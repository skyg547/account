package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsAccountRateRepository;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountRate;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsAccountRateEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsAccountRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsAccountRatePersistenceAdapter implements OdsAccountRateRepository {

    private final JpaOdsAccountRateRepository jpaRepository;

    @Override
    public Optional<OdsAccountRate> findById(String accountNo) {
        return jpaRepository.findById(accountNo).map(this::main);
    }

    @Override
    public List<OdsAccountRate> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public OdsAccountRate save(OdsAccountRate rate) {
        return main(jpaRepository.save(toEntity(rate)));
    }

    @Override
    public void saveAll(Iterable<OdsAccountRate> rates) {
        if (rates == null) {
            return;
        }
        List<OdsAccountRateEntity> entities = new java.util.ArrayList<>();
        rates.forEach(rate -> entities.add(toEntity(rate)));
        jpaRepository.saveAll(entities);
    }

    private OdsAccountRate main(OdsAccountRateEntity entity) {
        if (entity == null) {
            return null;
        }
        return OdsAccountRate.builder()
                .accountNo(entity.getAccountNo())
                .rateType(entity.getRateType())
                .baseRateCode(entity.getBaseRateCode())
                .spread(entity.getSpread())
                .appliedRate(entity.getAppliedRate())
                .interestRateCap(entity.getInterestRateCap())
                .interestRateFloor(entity.getInterestRateFloor())
                .refIndexCode(entity.getRefIndexCode())
                .paymentFreq(entity.getPaymentFreq())
                .rateResetCycle(entity.getRateResetCycle())
                .lastResetDate(entity.getLastResetDate())
                .nextResetDate(entity.getNextResetDate())
                .build();
    }

    private OdsAccountRateEntity toEntity(OdsAccountRate domain) {
        if (domain == null) {
            return null;
        }
        return OdsAccountRateEntity.builder()
                .accountNo(domain.getAccountNo())
                .rateType(domain.getRateType())
                .baseRateCode(domain.getBaseRateCode())
                .spread(domain.getSpread())
                .appliedRate(domain.getAppliedRate())
                .interestRateCap(domain.getInterestRateCap())
                .interestRateFloor(domain.getInterestRateFloor())
                .refIndexCode(domain.getRefIndexCode())
                .paymentFreq(domain.getPaymentFreq())
                .rateResetCycle(domain.getRateResetCycle())
                .lastResetDate(domain.getLastResetDate())
                .nextResetDate(domain.getNextResetDate())
                .build();
    }
}
