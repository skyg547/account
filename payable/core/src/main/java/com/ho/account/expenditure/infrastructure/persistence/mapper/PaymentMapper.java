package com.ho.account.expenditure.infrastructure.persistence.mapper;

import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Payment Domain POJO <-> PaymentJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * Payment 도메인 객체와 JPA 영속성 엔티티(PaymentJpaEntity) 간 양방향 데이터 매핑을 전담합니다.
 * PaymentRun 연관객체 변환 시 PaymentRunMapper를 활용하여 객체 그래프 변환을 매끄럽게 처리합니다.
 */
@Component
public class PaymentMapper {

    private final PaymentRunMapper paymentRunMapper;

    public PaymentMapper(PaymentRunMapper paymentRunMapper) {
        this.paymentRunMapper = paymentRunMapper;
    }

    public Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) return null;
        Payment domain = new Payment();
        domain.setId(entity.getId());
        domain.setPaymentDate(entity.getPaymentDate());
        domain.setVendorCode(entity.getVendorCode());
        domain.setPayableId(entity.getPayableId());
        domain.setAmount(entity.getAmount());
        domain.setBankAccount(entity.getBankAccount());
        domain.setReferenceNo(entity.getReferenceNo());
        domain.setExecutionAttempts(entity.getExecutionAttempts());
        domain.setFailureReason(entity.getFailureReason());
        domain.setStatus(entity.getStatus());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setPaymentRun(paymentRunMapper.toDomain(entity.getPaymentRun()));
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public PaymentJpaEntity toEntity(Payment domain) {
        if (domain == null) return null;
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(domain.getId());
        entity.setPaymentDate(domain.getPaymentDate());
        entity.setVendorCode(domain.getVendorCode());
        entity.setPayableId(domain.getPayableId());
        entity.setAmount(domain.getAmount());
        entity.setBankAccount(domain.getBankAccount());
        entity.setReferenceNo(domain.getReferenceNo());
        entity.setExecutionAttempts(domain.getExecutionAttempts());
        entity.setFailureReason(domain.getFailureReason());
        entity.setStatus(domain.getStatus());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setPaymentRun(paymentRunMapper.toEntity(domain.getPaymentRun()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
