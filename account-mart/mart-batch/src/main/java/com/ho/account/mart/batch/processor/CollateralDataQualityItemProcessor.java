package com.ho.account.mart.batch.processor;

import com.ho.account.mart.batch.support.BatchStepParameterUtils;
import com.ho.account.mart.core.application.service.ods.CollateralDataQualityInspectionService;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Batch Adapter] 담보 DQ Step에서 Batch row를 core 품질 유즈케이스로 전달한다.
 *
 * <p>초보자 설명: 이 클래스는 Spring Batch가 한 건씩 읽은 담보 row와 기준일을 core로 넘기는
 * 어댑터입니다. 담보 상세 조회나 LGD 선행값 판단은 core application/domain 계층이 수행합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class CollateralDataQualityItemProcessor implements ItemProcessor<OdsCollateralMst, OdsDqAudit>, StepExecutionListener {

    private final CollateralDataQualityInspectionService inspectionService;
    private LocalDate baseDate;

    @Override
    public void beforeStep(@NonNull StepExecution stepExecution) {
        this.baseDate = BatchStepParameterUtils.resolveBaseDate(stepExecution);
    }

    @Override
    @Nullable
    public OdsDqAudit process(@NonNull OdsCollateralMst collateral) {
        return inspectionService.inspect(collateral, baseDate);
    }

    @Override
    public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }
}
