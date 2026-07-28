package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsEarlyWarningRepository;
import com.ho.account.mart.core.domain.ods.loan.OdsEarlyWarning;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsEarlyWarningEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsEarlyWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsEarlyWarningPersistenceAdapter implements OdsEarlyWarningRepository {

    private final JpaOdsEarlyWarningRepository jpaRepository;

    @Override
    public Optional<OdsEarlyWarning> findTopByCustomerCodeAndBaseDateOrderByBaseDateDesc(
            String customerCode,
            LocalDate baseDate) {
        Optional<OdsEarlyWarningEntity> entity = baseDate == null
                ? jpaRepository.findTopByCustomerCodeOrderByBaseDateDesc(customerCode)
                : jpaRepository.findTopByCustomerCodeAndBaseDateLessThanEqualOrderByBaseDateDesc(customerCode, baseDate);
        return entity.map(this::main);
    }

    @Override
    public List<OdsEarlyWarning> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public OdsEarlyWarning save(OdsEarlyWarning warning) {
        return main(jpaRepository.save(toEntity(warning)));
    }

    @Override
    public void saveAll(Iterable<OdsEarlyWarning> warnings) {
        if (warnings == null) {
            return;
        }
        List<OdsEarlyWarningEntity> entities = new java.util.ArrayList<>();
        warnings.forEach(warning -> entities.add(toEntity(warning)));
        jpaRepository.saveAll(entities);
    }

    private OdsEarlyWarning main(OdsEarlyWarningEntity entity) {
        if (entity == null) {
            return null;
        }
        return OdsEarlyWarning.builder()
                .baseDate(entity.getBaseDate())
                .customerCode(entity.getCustomerCode())
                .warningLevel(entity.getWarningLevel())
                .warningReason(entity.getWarningReason())
                .warningScore(entity.getWarningScore())
                .build();
    }

    private OdsEarlyWarningEntity toEntity(OdsEarlyWarning domain) {
        if (domain == null) {
            return null;
        }
        return OdsEarlyWarningEntity.builder()
                .baseDate(domain.getBaseDate())
                .customerCode(domain.getCustomerCode())
                .warningLevel(domain.getWarningLevel())
                .warningReason(domain.getWarningReason())
                .warningScore(domain.getWarningScore())
                .build();
    }
}
