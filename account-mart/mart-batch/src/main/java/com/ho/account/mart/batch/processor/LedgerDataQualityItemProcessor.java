package com.ho.account.mart.batch.processor;

import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.audit.processor.LedgerDataQualityProcessor;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import com.ho.account.mart.batch.support.BatchStepParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Batch Adapter] 원장 DQ Step에서 Batch row를 core 품질 규칙으로 전달한다.
 */
@Component
@RequiredArgsConstructor
public class LedgerDataQualityItemProcessor implements ItemProcessor<OdsAccountLedger, OdsDqAudit>, StepExecutionListener {

    private final LedgerDataQualityProcessor delegate;
    private LocalDate baseDate;

    @Override
    public void beforeStep(@NonNull StepExecution stepExecution) {
        this.baseDate = BatchStepParameterUtils.resolveBaseDate(stepExecution);
    }

    @Override
    public OdsDqAudit process(@NonNull OdsAccountLedger ledger) {
        return delegate.inspect(ledger, baseDate);
    }

    @Override
    public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }
}
