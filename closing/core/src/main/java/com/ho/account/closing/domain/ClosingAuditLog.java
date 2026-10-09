package com.ho.account.closing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [ClosingAuditLog]
 * 결산 프로세스 중 발생하는 주요 상태 변경 및 사용자 행위를 기록하는 감사 로그 엔티티.
 * 금융 시스템의 투명성을 위해 결산의 확정, 취소, 재오픈 사유 등을 영구 보존합니다.
 */
@Entity
@Table(name = "closing_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class ClosingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ClosingCalendar closingCalendar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActionType actionType; // CALENDAR_OPEN, CALENDAR_CLOSED, PERIOD_REOPENED, TASK_COMPLETED, etc.

    @Column(length = 50)
    private String previousStatus;

    @Column(nullable = false, length = 50)
    private String currentStatus;

    @Column(length = 1000)
    private String actionReason; // 작업 사유 (특히 취소나 재오픈 시 필수)

    @Column(nullable = false, length = 50)
    private String actionUser;

    @Column(nullable = false)
    private LocalDateTime actionAt;

    @Column(columnDefinition = "TEXT")
    private String detailInfo; // 추가적인 변경 내역(JSON 등)

    public enum ActionType {
        CALENDAR_OPEN,
        CALENDAR_IN_PROGRESS,
        CALENDAR_CLOSED,
        CALENDAR_PERMANENTLY_CLOSED,
        PERIOD_LOCK,
        PERIOD_UNLOCK,
        REOPEN_REQUEST,
        REOPEN_APPROVED,
        REOPEN_REJECTED,
        TASK_STATUS_CHANGED,
        GATE_PASSED,
        ADJUSTMENT_CREATED,
        PERIOD_TRANSITION_PREPARED,
        PERIOD_TRANSITION_CANCELLED,
        PERIOD_TRANSITION_RECOVERED
    }

    @PrePersist
    protected void onCreate() {
        this.actionAt = LocalDateTime.now();
    }

    public static ClosingAuditLog create(ClosingCalendar calendar, ActionType type, String prevStatus, String currStatus, String user, String reason) {
        ClosingAuditLog log = new ClosingAuditLog();
        log.setClosingCalendar(calendar);
        log.setActionType(type);
        log.setPreviousStatus(prevStatus);
        log.setCurrentStatus(currStatus);
        log.setActionUser(user);
        log.setActionReason(reason);
        return log;
    }
}
