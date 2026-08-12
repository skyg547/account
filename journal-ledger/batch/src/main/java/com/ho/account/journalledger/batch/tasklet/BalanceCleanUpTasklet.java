package com.ho.account.journalledger.batch.tasklet;

import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * GL/SL 잔액 재집계 전 사전 Clean-up Tasklet.
 *
 * <p><b>[교육적 설명 및 아키텍처 아젠다]</b><br>
 * 배치 재실행(Restartability) 시 동일 기간의 잔액을 중복으로 계산하여 더하는 부작용(Side Effect)을 막기 위해,
 * 배치 메인 청크 단계(Chunk Step) 진입 전 대상 기간의 기존 GL/SL 잔액 데이터를 깨끗이 지우는 멱등성(Idempotency) 보장 전처리 단계입니다.
 * 
 * <ul>
 *   <li><b>단일 책임 원칙 (SRP):</b> 재집계 조작 전 사전 삭제 전처리만 묵묵히 수행합니다.</li>
 *   <li><b>멱등성 (Idempotency):</b> 장애 발생 등으로 배치를 재실행해도 항상 동일한 상태에서 다시 재집계를 시작할 수 있습니다.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceCleanUpTasklet implements Tasklet {

    private final LedgerService ledgerService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveDateRange(contribution.getStepExecution());

        log.info("[Journal Ledger Batch] Clean-up existing GL/SL balances for re-aggregation. startDate={}, endDate={}",
                range.startDate(), range.endDate());
        ledgerService.clearLedgerBalancesForPeriod(range.startDate(), range.endDate());
        log.info("[Journal Ledger Batch] Clean-up existing GL/SL balances completed. startDate={}, endDate={}",
                range.startDate(), range.endDate());

        return RepeatStatus.FINISHED;
    }
}
