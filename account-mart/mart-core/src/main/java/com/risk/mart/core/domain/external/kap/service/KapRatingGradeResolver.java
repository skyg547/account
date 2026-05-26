package com.risk.mart.core.domain.external.kap.service;

import com.risk.mart.core.application.port.out.RatingGradeMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * [Service] 외부 등급 번역 및 정규화 엔진 (KapRatingGradeResolver)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 외부 평가기관(KAP 등)에서 보내주는 제각각인 등급 이름을 
 * 우리 리스크 시스템이 이해할 수 있는 '표준 이름'으로 고쳐주는 일을 합니다.
 * 
 * 예시:
 * - "A Plus" 또는 "A_PLUS"가 들어오면 -> "A+"로 변환
 * - "N/R" 또는 "NOT RATED"가 들어오면 -> "NR"(무등급)로 변환
 * 
 * 이렇게 정규화를 거쳐야 리스크 엔진이 헷갈리지 않고 정확한 부도율(PD)을 찾아낼 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KapRatingGradeResolver {

    private static final String NOT_RATED = "NR";
    private static final String PLUS_SUFFIX = "_PLUS";
    private static final String MINUS_SUFFIX = "_MINUS";

    private final RatingGradeMasterRepository ratingGradeMasterRepository;

    /**
     * 날것의 등급 텍스트를 받아서 표준 등급 코드로 변환합니다.
     */
    public String resolveGradeCode(String rawValue) {
        String normalized = normalize(rawValue);

        return ratingGradeMasterRepository.findByGradeCodeIgnoreCaseAndIsActiveTrue(normalized)
                .map(grade -> grade.getGradeCode().toUpperCase(Locale.ROOT))
                .orElseGet(() -> {
                    log.warn("⚠️ [KAP 등급] '{}'를 표준 마스터 테이블에서 찾지 못했습니다. NR(무등급)로 대체합니다.", rawValue);
                    return ratingGradeMasterRepository.findByGradeCodeIgnoreCaseAndIsActiveTrue(NOT_RATED)
                            .map(grade -> grade.getGradeCode().toUpperCase(Locale.ROOT))
                            .orElse(NOT_RATED);
                });
    }

    /**
     * 복잡한 문자열 패턴을 단순한 표준 기호(+, - 등)로 정리합니다.
     */
    private String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return NOT_RATED;
        }

        String normalized = rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');

        // 무등급 관련 다양한 표현을 하나로 통일
        if ("N/R".equals(normalized) || "NOT_RATED".equals(normalized) || "UNRATED".equals(normalized)) {
            return NOT_RATED;
        }

        // 접미사 처리 (예: AAA_PLUS -> AAA+)
        if (normalized.endsWith(PLUS_SUFFIX)) {
            return normalized.substring(0, normalized.length() - PLUS_SUFFIX.length()) + "+";
        }

        if (normalized.endsWith(MINUS_SUFFIX)) {
            return normalized.substring(0, normalized.length() - MINUS_SUFFIX.length()) + "-";
        }

        return normalized.replace("_", "");
    }
}
