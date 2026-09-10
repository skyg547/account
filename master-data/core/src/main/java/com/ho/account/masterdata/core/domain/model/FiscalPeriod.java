package com.ho.account.masterdata.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 회계기간(Fiscal Period) 도메인 모델
 * 특정 연도의 월별 마감 상태를 관리합니다.
 * 
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO 원칙:
 *    회계기간 도메인 모델은 JPA 어노테이션에 의존하지 않는 순수 자바 객체로서 비즈니스 인메모리 행위(changeClosingStatus)에 집중합니다.
 * 2. 도메인-영속성 모델 분리:
 *    FiscalPeriodEntity와 Data Mapper를 통해 DB 영속화 방식을 분리합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class FiscalPeriod {

    private Long id;
    private String fiscalYear;
    private String fiscalPeriod;
    private LocalDate startDate;
    private LocalDate endDate;

    @Setter(AccessLevel.NONE)
    private ClosingStatus closingStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public enum ClosingStatus {
        OPEN, CLOSED, PERMANENTLY_CLOSED
    }

    /**
     * 저장된 값을 새 업무 전이 없이 복원합니다. 영구 마감 행을 읽는 것은 영구 마감 명령이 아닙니다.
     * 감사 시간과 사용자(공백/null 포함)는 당시 값 그대로 두며, 상태 누락은 OPEN으로 숨기지 않습니다.
     * 새 상태 변경은 반드시 changeClosingStatus를 사용합니다. 전이 계약은 master-data/docs/schema.md 참조.
     */
    public static FiscalPeriod reconstitute(
            Long id, String fiscalYear, String fiscalPeriod, LocalDate startDate, LocalDate endDate,
            ClosingStatus closingStatus, LocalDateTime createdAt, LocalDateTime updatedAt, String auditUser) {
        FiscalPeriod restored = new FiscalPeriod();
        restored.id = id;
        restored.fiscalYear = fiscalYear;
        restored.fiscalPeriod = fiscalPeriod;
        restored.startDate = startDate;
        restored.endDate = endDate;
        restored.closingStatus = Objects.requireNonNull(closingStatus, "Closing status is required.");
        restored.createdAt = createdAt;
        restored.updatedAt = updatedAt;
        restored.auditUser = auditUser;
        return restored;
    }

    /**
     * 회계기간 상태를 도메인 규칙과 감사 사용자 검증을 거쳐 변경합니다.
     * 영구 마감은 되돌릴 수 없고, 열린 기간은 일반 마감을 거치지 않고 영구 마감할 수 없습니다.
     */
    public void changeClosingStatus(ClosingStatus nextStatus, String changedBy) {
        ClosingStatus next = Objects.requireNonNull(nextStatus, "Closing status is required.");
        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("Closing status audit user is required.");
        }

        ClosingStatus current = closingStatus == null ? ClosingStatus.OPEN : closingStatus;
        if (current == ClosingStatus.PERMANENTLY_CLOSED && next != current) {
            throw new IllegalStateException("Permanently closed fiscal period cannot be reopened or changed.");
        }
        if (current == ClosingStatus.OPEN && next == ClosingStatus.PERMANENTLY_CLOSED) {
            throw new IllegalStateException("Fiscal period must be closed before permanent closing.");
        }

        this.closingStatus = next;
        this.auditUser = changedBy.trim();
        this.updatedAt = LocalDateTime.now();
    }

    @Deprecated
    public String getFiscalYearAndPeriod() {
        return fiscalYear + "-" + fiscalPeriod;
    }
}

