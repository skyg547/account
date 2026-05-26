package com.risk.mart.core.infrastructure.persistence;

import com.risk.mart.core.application.port.out.OdsGeneralLedgerRepository;
import com.risk.mart.core.domain.ods.common.OdsGeneralLedger;
import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsGeneralLedgerEntity;
import com.risk.mart.core.infrastructure.persistence.jpa.JpaOdsGeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsGeneralLedgerPersistenceAdapter implements OdsGeneralLedgerRepository {

    private final JpaOdsGeneralLedgerRepository jpaRepository;

    @Override
    public List<OdsGeneralLedger> findByBaseDate(LocalDate baseDate) {
        return jpaRepository.findByBaseDate(baseDate).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<OdsGeneralLedger> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public OdsGeneralLedger save(OdsGeneralLedger domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public void saveAll(List<OdsGeneralLedger> domains) {
        if (domains == null) return;
        List<OdsGeneralLedgerEntity> entities = domains.stream()
                .map(d -> toEntity(d))
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<SubjectCurrencyBalanceSummary> getBalanceSummaryByBaseDate(LocalDate baseDate) {
        // [임시 구현] 리포지토리에서 쿼리 지원 필요 시 추가. 여기서는 수동 그룹핑 처리 예시
        return jpaRepository.findByBaseDate(baseDate).stream()
                .map(e -> new SubjectCurrencyBalanceSummary() {
                    @Override public String getSubjectCode() { return e.getGlCode(); }
                    @Override public String getCurrencyCode() { return e.getCurrency(); }
                    @Override public java.math.BigDecimal getBalanceAmount() { return e.getBalance(); }
                })
                .collect(Collectors.toList());
    }

    private OdsGeneralLedger toDomain(OdsGeneralLedgerEntity entity) {
        if (entity == null) return null;
        return OdsGeneralLedger.builder()
                .baseDate(entity.getBaseDate())
                .glCode(entity.getGlCode())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .branchCode(entity.getBranchCode())
                .build();
    }

    private OdsGeneralLedgerEntity toEntity(OdsGeneralLedger domain) {
        if (domain == null) return null;
        return OdsGeneralLedgerEntity.builder()
                .baseDate(domain.getBaseDate())
                .glCode(domain.getGlCode())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .branchCode(domain.getBranchCode())
                .build();
    }
}
