package com.ho.account.mart.batch.support;

import com.ho.account.mart.core.support.BatchParameterUtils;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;

/**
 * Spring Batch의 StepExecution에서 업무 기준일을 꺼내 core가 이해하는 단순 문자열로 바꾼다.
 *
 * <p>초보자 설명: batch 모듈은 Spring Batch 기술 객체를 알고 있어도 되지만,
 * core 모듈은 업무 규칙을 테스트하기 쉽게 문자열/날짜 같은 기본 타입만 받도록 유지한다.
 */
public final class BatchStepParameterUtils {

    private BatchStepParameterUtils() {
    }

    public static LocalDate resolveBaseDate(StepExecution stepExecution) {
        return BatchParameterUtils.resolveBaseDate(
                stepExecution.getJobParameters().getString("baseDt"),
                stepExecution.getJobParameters().getString("baseDate")
        );
    }
}
