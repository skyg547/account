package com.ho.account.mart.core.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 배치 기준일 파라미터 해석 유틸리티.
 * ODS/CDM 배치는 baseDate(or baseDt) 파라미터를 필수로 사용한다.
 *
 * <p>초보자 설명: core는 Spring Batch의 StepExecution 같은 기술 객체를 직접 알지 않는다.
 * Batch adapter가 문자열 파라미터만 넘기고, core는 "어떤 기준일로 업무 판단을 할지"만 해석한다.</p>
 */
public final class BatchParameterUtils {

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private BatchParameterUtils() {
    }

    public static LocalDate resolveBaseDate(String baseDt, String baseDate) {
        String resolvedBaseDate = firstNonBlank(baseDt, baseDate)
                .orElseThrow(() -> new IllegalArgumentException("baseDate(or baseDt) job parameter is required"));
        return LocalDate.parse(resolvedBaseDate, DEFAULT_DATE_FORMAT);
    }

    private static java.util.Optional<String> firstNonBlank(String... candidates) {
        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate != null && !candidate.isBlank()) {
                    return java.util.Optional.of(candidate.trim());
                }
            }
        }
        return java.util.Optional.empty();
    }
}
