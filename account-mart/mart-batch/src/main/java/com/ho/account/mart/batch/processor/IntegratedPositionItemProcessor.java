package com.ho.account.mart.batch.processor;

import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessor;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import com.ho.account.mart.core.infrastructure.persistence.entity.mart.AllowanceInputPositionEntity;
import com.ho.account.mart.core.infrastructure.persistence.mapper.AllowanceInputPositionMapper;
import com.ho.account.mart.batch.support.BatchStepParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Batch Adapter] Spring Batch가 읽은 원장 row를 core CDM 변환 규칙에 위임하고, JPA 영속성 엔티티로 매핑한다.
 *
 * <p>초보자 설명: Batch 기술 객체(StepExecution, ItemProcessor)는 이 adapter에서만 다룬다.
 * 실제 IFRS 9 입력 포지션 변환 규칙은 mart-core의 {@link IntegratedPositionProcessor}가 담당하며,
 * 변환된 도메인 POJO 모델을 DB 적재를 위해 {@link AllowanceInputPositionMapper}로 Entity 변환하여 반환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegratedPositionItemProcessor implements ItemProcessor<OdsAccountLedger, AllowanceInputPositionEntity>, StepExecutionListener {

    private final IntegratedPositionProcessor delegate;
    private LocalDate baseDate;

    @Override
    public void beforeStep(@NonNull StepExecution stepExecution) {
        this.baseDate = BatchStepParameterUtils.resolveBaseDate(stepExecution);
        log.info("🚀 [CDM] 대손충당금 입력 포지션 변환 공정을 시작합니다. (기준일: {})", baseDate);
    }

    @Override
    public AllowanceInputPositionEntity process(@NonNull OdsAccountLedger ledger) {
        AllowanceInputPosition domain = delegate.process(ledger, baseDate);
        return AllowanceInputPositionMapper.toEntity(domain);
    }

    @Override
    public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
        log.info("✅ [CDM] 대손충당금 입력 포지션 변환 완료. (Status: {})", stepExecution.getStatus());
        return ExitStatus.COMPLETED;
    }
}

