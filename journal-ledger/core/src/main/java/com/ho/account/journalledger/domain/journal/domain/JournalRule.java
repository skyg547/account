package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 자동 분개 규칙 (Journal Rule) — 이벤트 기반 전표 자동 생성 규칙 정의.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 자동 분개 규칙은 특정 업무 이벤트(예: 매입 인보이스 등록, 리스 지급 등)가 발생했을 때
 * 사람이 수동으로 전표를 작성하지 않아도, 시스템이 미리 정의된 규칙에 따라
 * 자동으로 분개 전표를 생성할 수 있도록 합니다.
 *
 * 하나의 규칙은:
 *   1. 조건(JournalRuleCondition 목록) — 이 규칙이 적용될 이벤트 조건
 *   2. 상세(JournalRuleDetail 목록)  — 생성할 전표 라인(차/대변 계정, 금액 표현식)
 * 로 구성됩니다.
 *
 * 주요 필드:
 *   - ruleCode   : 규칙 식별 코드 (예: "PURCHASE_RECOGNITION", "LEASE_PAYMENT")
 *   - validFrom/validTo : 규칙 유효 기간 (회계 기준 변경 시 기간 지정)
 *   - priority   : 여러 규칙이 조건에 맞을 때 우선 적용 순서 (숫자가 낮을수록 높은 우선순위)
 *   - version    : 규칙 이력 관리용 버전 번호
 *   - isActive   : 현재 활성 여부 (false이면 규칙 엔진이 무시)
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - JPA @Entity로 journal_rules 테이블에 매핑됩니다.
 * - JournalRuleEngine.generateJournalEntry()에서 이 규칙을 조회하여 전표를 생성합니다.
 * - conditions와 ruleDetails는 CascadeType.ALL + orphanRemoval=true로 관리합니다.
 * - 현재 JournalRuleEngine 구현은 스텁(stub) 상태이며, 실제 규칙 평가 로직은
 *   추후 conditions/ruleDetails를 순회하며 구현해야 합니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "journal_rules")
public class JournalRule {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 규칙 코드 (유니크).
     * 이벤트 데이터의 transactionType 또는 ruleCode 필드와 매칭됩니다.
     * 예: "PURCHASE_RECOGNITION", "LEASE_PAYMENT", "ASSET_DEPRECIATION"
     */
    @Column(nullable = false, unique = true, length = 50)
    private String ruleCode;

    /**
     * 규칙 이름 (사람이 읽을 수 있는 명칭).
     * 예: "매입 인식 분개", "리스 지급 분개", "감가상각 분개"
     */
    @Column(nullable = false, length = 100)
    private String ruleName;

    /** 규칙 설명 — 이 규칙이 어떤 상황에 적용되는지 상세 기술 */
    @Column(length = 500)
    private String description;

    /**
     * 규칙 유효 시작일.
     * 이 날짜 이후의 이벤트에 대해서만 이 규칙이 적용됩니다.
     * 회계 기준 변경(예: 새 회계연도) 시 새 규칙을 만들고 validFrom을 설정합니다.
     */
    @Column(nullable = false)
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
     * 초기값은 1 (@PrePersist).
     */
    @Column(nullable = false)
    private int version;

    /**
     * 활성 여부.
     * false이면 JournalRuleEngine이 이 규칙을 무시합니다.
     * 규칙을 임시 비활성화할 때 사용합니다.
     */
    @Column(nullable = false)
    private boolean isActive;

    /**
     * 우선순위 (Priority).
     * 동일한 이벤트에 여러 규칙이 매칭될 때 priority가 낮은 규칙이 먼저 평가됩니다.
     * (숫자 작을수록 = 높은 우선순위)
     * 기본값: 999 (@PrePersist) — 명시적으로 설정하지 않으면 가장 낮은 우선순위.
     */
    @Column(nullable = false)
    private int priority;

    /**
     * 이 규칙이 적용되기 위한 조건 목록.
     * 모든 조건이 AND 방식으로 충족되어야 이 규칙이 적용됩니다.
     * JournalRuleCondition을 참고하세요.
     */
    @OneToMany(mappedBy = "journalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalRuleCondition> conditions = new ArrayList<>();

    /**
     * 이 규칙으로 생성할 전표 라인(분개 명세) 목록.
     * 각 라인은 차/대변 구분, 계정과목 표현식, 금액 표현식을 포함합니다.
     * JournalRuleDetail을 참고하세요.
     */
    @OneToMany(mappedBy = "journalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalRuleDetail> ruleDetails = new ArrayList<>();

    /** 최초 생성 일시 (수정 불가) */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    /** 최종 처리자 */
    @Column(length = 50)
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
    @PrePersist
    protected void onCreate() {
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
    @PreUpdate
    protected void onUpdate() {
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

    // ─── Getter / Setter ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<JournalRuleCondition> getConditions() { return conditions; }
    public void setConditions(List<JournalRuleCondition> conditions) { this.conditions = conditions; }

    public List<JournalRuleDetail> getRuleDetails() { return ruleDetails; }
    public void setRuleDetails(List<JournalRuleDetail> ruleDetails) { this.ruleDetails = ruleDetails; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
