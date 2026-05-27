package com.risk.mart.core.infrastructure.persistence;

import com.risk.mart.core.application.port.out.OdsBalanceHistRepository;
import com.risk.mart.core.domain.ods.common.OdsBalanceHist;
import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsBalanceHistEntity;
import com.risk.mart.core.infrastructure.persistence.jpa.JpaOdsBalanceHistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsBalanceHistPersistenceAdapter implements OdsBalanceHistRepository {

    private final JpaOdsBalanceHistRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

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
        return jdbcTemplate.query("""
                        SELECT p.subj_cd AS subject_code,
                               b.currency AS currency_code,
                               SUM(b.balance) AS balance_amount
                          FROM ods_balance_hist b
                          JOIN ods_acc_ledger a ON a.acc_no = b.account_no
                          JOIN ods_product_mst p ON p.prod_cd = a.prod_cd
                         WHERE b.base_dt = ?
                         GROUP BY p.subj_cd, b.currency
                         ORDER BY p.subj_cd, b.currency
                        """,
                (rs, rowNum) -> {
                    String subjectCode = rs.getString("subject_code");
                    String currencyCode = rs.getString("currency_code");
                    java.math.BigDecimal balanceAmount = rs.getBigDecimal("balance_amount");
                    return new BalanceSummary() {
                    @Override
                    public String getSubjectCode() {
                        return subjectCode;
                    }

                    @Override
                    public String getCurrencyCode() {
                        return currencyCode;
                    }

                    @Override
                    public java.math.BigDecimal getBalanceAmount() {
                        return balanceAmount;
                    }
                    };
                },
                java.sql.Date.valueOf(baseDate));
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
