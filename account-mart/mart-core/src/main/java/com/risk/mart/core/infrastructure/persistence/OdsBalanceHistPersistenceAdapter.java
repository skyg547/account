package com.risk.mart.core.infrastructure.persistence;

import com.risk.mart.core.application.port.out.OdsBalanceHistRepository;
import com.risk.mart.core.domain.ods.common.OdsBalanceHist;
import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsBalanceHistEntity;
import com.risk.mart.core.infrastructure.persistence.jpa.JpaOdsBalanceHistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsBalanceHistPersistenceAdapter implements OdsBalanceHistRepository {

    private final JpaOdsBalanceHistRepository jpaRepository;

    @Override
    public Optional<OdsBalanceHist> findTopByAccountNoOrderByBaseDateDesc(String accountNo) {
        return jpaRepository.findTopByAccountNoOrderByBaseDateDesc(accountNo).map(e -> toDomain(e));
    }

    @Override
    public List<OdsBalanceHist> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public OdsBalanceHist save(OdsBalanceHist domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public void saveAll(List<OdsBalanceHist> domains) {
        if (domains == null) return;
        List<OdsBalanceHistEntity> entities = domains.stream()
                .map(d -> toEntity(d))
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<BalanceSummary> findBalanceSummaryByBaseDate(LocalDate baseDate) {
        // [임시 구현] 요약 인터페이스 익명 클래스 구현
        return jpaRepository.findAll().stream() // 필터링은 리포지토리 보강 권장
                .filter(e -> e.getBaseDate().equals(baseDate))
                .map(e -> new BalanceSummary() {
                    @Override public String getSubjectCode() { return e.getAccountNo(); }
                    @Override public String getCurrencyCode() { return e.getCurrency(); }
                    @Override public java.math.BigDecimal getBalanceAmount() { return e.getBalance(); }
                })
                .collect(Collectors.toList());
    }

    private OdsBalanceHist toDomain(OdsBalanceHistEntity entity) {
        if (entity == null) return null;
        return OdsBalanceHist.builder()
                .baseDate(entity.getBaseDate())
                .accountNo(entity.getAccountNo())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .build();
    }

    private OdsBalanceHistEntity toEntity(OdsBalanceHist domain) {
        if (domain == null) return null;
        return OdsBalanceHistEntity.builder()
                .baseDate(domain.getBaseDate())
                .accountNo(domain.getAccountNo())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .build();
    }
}
