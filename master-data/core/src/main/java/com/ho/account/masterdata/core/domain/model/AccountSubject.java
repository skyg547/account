package com.ho.account.masterdata.core.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계정과목(Account Subject) 도메인 모델.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 관리한다.
 * 유일한 계정코드(code)가 존재하더라도 연관관계는 대체 키인 기술적인 기본키(id)를 사용한다.
 * 
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO (Plain Old Java Object) 원칙:
 *    도메인 모델은 데이터베이스나 JPA(jakarta.persistence.*) 등의 특정 기술/프레임워크 어노테이션에 의존하지 않는 순수한 자바 객체여야 합니다.
 *    이를 통해 도메인 로직이 데이터베이스 테이블 스키마나 ORM 매핑 기술에 결합되지 않고 독립적으로 테스트 및 확장될 수 있습니다.
 * 2. 도메인-영속성 모델 분리 (Domain vs Entity):
 *    - Domain Model (AccountSubject): 비즈니스 규칙, 상태 변경 메서드(terminate 등), 검증 로직을 포함하는 핵심 객체.
 *    - JPA Entity (AccountSubjectEntity): DB 테이블 구조(컬럼, 인덱스, FK, JPA 생명주기 어노테이션)를 표현하는 영속성 전용 객체.
 *     infrastructure 패키지의 Data Mapper (AccountSubjectMapper)를 통해 두 모델 간 변환을 수행합니다.
 * 
 * 계정과목은 회사에서 돈이 들어오고 나가는 명목(이름표)을 뜻합니다.
 * 이 클래스는 시스템에서 사용되는 모든 계정과목표의 마스터 데이터를 정의하며,
 * 과거 이력을 모두 보존하는 SCD2 방식을 지원합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountSubject {

    private Long id;
    private String code; // 계정코드 (유일 코드가 아님에 주의)
    private String name; // 계정과목명
    private AccountSubject parent; // 상위 계정과목 (ID 기반 연관관계)
    private AccountCategory category;
    private AccountType accountType;
    private BalanceType balanceType;
    private String reportLine;
    private String regulatoryMappingCode;
    private boolean unsettled;
    private LocalDate validFrom;
    private LocalDate validTo;
    private boolean fixedAsset;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public enum AccountCategory {
        ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES
    }

    public enum AccountType {
        ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES,
        NON_OPERATING_INCOME, NON_OPERATING_EXPENSES
    }

    public enum BalanceType {
        DEBIT, CREDIT
    }

    /**
     * 기존 코드와의 호환성을 위해 남겨둔 메서드들입니다.
     */
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    @Deprecated
    public void setAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            this.accountType = null;
            return;
        }

        String normalized = switch (accountType) {
            case "ASSET" -> "ASSETS";
            case "LIABILITY" -> "LIABILITIES";
            case "EXPENSE" -> "EXPENSES";
            default -> accountType;
        };
        this.accountType = AccountType.valueOf(normalized);
    }


    @Deprecated
    public void setDescription(String description) {
        this.reportLine = description;
    }

    /**
     * 특정 시점에 유효한지 확인
     */
    public boolean isValid(LocalDate date) {
        return (date.isEqual(validFrom) || date.isAfter(validFrom)) && (date.isEqual(validTo) || date.isBefore(validTo));
    }

    /**
     * 현재 이력을 종료
     */
    public void terminate(LocalDate endDate) {
        this.validTo = endDate;
        this.updatedAt = LocalDateTime.now();
    }
}

