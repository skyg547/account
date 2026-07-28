package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsGeneralLedgerRepository;
import com.ho.account.mart.core.domain.ods.common.OdsGeneralLedger;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsGeneralLedgerEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsGeneralLedgerRepository;
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
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OdsGeneralLedger> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public OdsGeneralLedger save(OdsGeneralLedger domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public void saveAll(List<OdsGeneralLedger> domains) {
        if (domains == null) {
            return;
        }
        List<OdsGeneralLedgerEntity> entities = domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<SubjectCurrencyBalanceSummary> getBalanceSummaryByBaseDate(LocalDate baseDate) {
        // [초보 가이드] GL/SL 대사는 계정+통화 단위 합계가 맞아야 한다.
        // 행 단위 데이터를 그대로 넘기면 같은 계정/통화가 여러 지점에 나뉜 경우 차이가 왜곡되므로 DB에서 group-by 한다.
        return jpaRepository.getBalanceSummaryByBaseDate(baseDate);
    }

    private OdsGeneralLedger toDomain(OdsGeneralLedgerEntity entity) {
        if (entity == null) {
            return null;
        }
        return OdsGeneralLedger.builder()
                .baseDate(entity.getBaseDate())
                .glCode(entity.getGlCode())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .branchCode(entity.getBranchCode())
                .build();
    }

    private OdsGeneralLedgerEntity toEntity(OdsGeneralLedger domain) {
        if (domain == null) {
            return null;
        }
        return OdsGeneralLedgerEntity.builder()
                .baseDate(domain.getBaseDate())
                .glCode(domain.getGlCode())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .branchCode(domain.getBranchCode())
                .build();
    }
}
