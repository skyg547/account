package com.ho.account.journalledger.domain.journal.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자동 분개 규칙 (Journal Rule) — 이벤트 기반 전표 자동 생성 규칙 정의.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 규칙의 이력을 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템이 전표를 자동으로 대신 써주기 위한 '매뉴얼'입니다.
 * 예를 들어 "영업팀에서 10만원짜리 비품을 샀다"는 데이터가 들어오면,
 * 이 룰이 발동해서 "차변: 소모품비 10만원, 대변: 미지급금 10만원" 이라는
 * 정확한 회계 전표(JournalEntry)를 어떻게 만들지 정의해 둡니다.
 * 회계 기준이 바뀌면 과거 규칙은 보존하고 새 규칙을 만들기 위해 유효기간(SCD2)을 사용합니다.
 */

@Getter
@Setter
@NoArgsConstructor
public class JournalRule {

    /** 시스템 내부 PK (자동 증가) */
    private Long id;

    /**
     * 규칙 코드 (SCD2를 위해 유니크 제약 조건 제거).
     * 이벤트 데이터의 transactionType 또는 ruleCode 필드와 매칭됩니다.
     */
    private String ruleCode;

    /**
     * 규칙 이름 (사람이 읽을 수 있는 명칭).
     * 예: "매입 인식 분개", "리스 지급 분개", "감가상각 분개"
     */
    private String ruleName;

    /** 규칙 설명 — 이 규칙이 어떤 상황에 적용되는지 상세 기술 */
    private String description;

    /**
     * 규칙 유효 시작일.
     * 이 날짜 이후의 이벤트에 대해서만 이 규칙이 적용됩니다.
     * 회계 기준 변경(예: 새 회계연도) 시 새 규칙을 만들고 validFrom을 설정합니다.
     */
    private LocalDate validFrom;

    /**
     * 규칙 유효 종료일.
     * null이면 현재 유효한 규칙입니다.
     * 규칙을 폐기할 때 이 날짜를 설정하여 이력을 보존합니다.
     */
    private LocalDate validTo;

    /**
     * 규칙 버전 번호.
     * 동일한 ruleCode로 여러 버전이 존재할 수 있습니다 (유효 기간으로 구분).
     * 초기값은 1 (최초 저장 시).
     */
    private int version;

    /**
     * 활성 여부.
     * false이면 JournalRuleEngine이 이 규칙을 무시합니다.
     * 규칙을 임시 비활성화할 때 사용합니다.
     */
    private boolean isActive;

    /**
     * 우선순위 (Priority).
     * 동일한 이벤트에 여러 규칙이 매칭될 때 priority가 낮은 규칙이 먼저 평가됩니다.
     * (숫자 작을수록 = 높은 우선순위)
     * 기본값: 999 (최초 저장 시) — 명시적으로 설정하지 않으면 가장 낮은 우선순위.
     */
    private int priority;

    /**
     * 이 규칙이 적용되기 위한 조건 목록.
     * 모든 조건이 AND 방식으로 충족되어야 이 규칙이 적용됩니다.
     * JournalRuleCondition을 참고하세요.
     */
    private List<JournalRuleCondition> conditions = new ArrayList<>();

    /**
     * 이 규칙으로 생성할 전표 라인(분개 명세) 목록.
     * 각 라인은 차/대변 구분, 계정과목 표현식, 금액 표현식을 포함합니다.
     * JournalRuleDetail을 참고하세요.
     */
    private List<JournalRuleDetail> ruleDetails = new ArrayList<>();

    /** 최초 생성 일시 (수정 불가) */
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    /** 최종 처리자 */
    private String auditUser;

    /** 규칙 등록자 */
    private String createdBy;

    // ─── 생명주기 콜백 ──────────────────────────────────────

    /**
     * 최초 저장 시 기본값 설정:
     * - validFrom: 오늘 날짜
     * - version: 1
     * - priority: 999 (가장 낮은 우선순위)
     * - ruleCode, ruleName이 null이면 예외 발생
     */
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.version == 0) this.version = 1;
        if (this.priority == 0) this.priority = 999;
        if (this.ruleCode == null) throw new IllegalArgumentException("Rule code cannot be null");
        if (this.ruleName == null) throw new IllegalArgumentException("Rule name cannot be null");
        if (this.auditUser == null) this.auditUser = this.createdBy != null ? this.createdBy : "SYSTEM";
    }

    /** 수정 시 updatedAt 자동 갱신 */
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── 도메인 편의 메서드 ──────────────────────────────────

    /**
     * 조건을 추가하고 양방향 연관관계를 설정합니다.
     * @param condition 추가할 규칙 조건
     */
    public void addCondition(JournalRuleCondition condition) {
        conditions.add(condition);
        condition.setJournalRule(this);
    }

    /**
     * 분개 명세 라인을 추가하고 양방향 연관관계를 설정합니다.
     * @param ruleDetail 추가할 분개 명세 라인
     */
    public void addRuleDetail(JournalRuleDetail ruleDetail) {
        ruleDetails.add(ruleDetail);
        ruleDetail.setJournalRule(this);
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
