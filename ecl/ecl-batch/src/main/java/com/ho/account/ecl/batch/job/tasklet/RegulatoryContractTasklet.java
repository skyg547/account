package com.ho.account.ecl.batch.job.tasklet;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.service.crm.RegulatoryContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [규제] 여신신청 및 계약/담보 관계 생성 태스크렛 (Regulatory Contract Tasklet)
 *
 * [초보자를 위한 개념 설명]
 * 이 태스크렛은 여신신청, 보증서, 담보-계좌 관계 같은 계약성 데이터를 규제 산출용 구조로 맞춰 주는 단계입니다.
 * 이후 담보 배분과 본산출이 가능하도록 기초 관계 데이터를 먼저 정리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegulatoryContractTasklet implements Tasklet {

    private final RegulatoryContractService regulatoryContractService;

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
        log.info("🚀 [Batch Step] 규제 계약-담보 매핑 프로세스 실행 중...");

        int count = regulatoryContractService.initializeRegulatoryContracts(baseDate);

        log.info("✅ [Batch Step] {}건의 데이터 매핑이 완료되었습니다.", count);
        return RepeatStatus.FINISHED;
    }
}
