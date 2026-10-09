package com.ho.account.closing.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [ClosingAuditLog]
 * 결산 프로세스 중 발생하는 주요 상태 변경 및 사용자 행위를 기록하는 감사 로그 엔티티.
 * 금융 시스템의 투명성을 위해 결산의 확정, 취소, 재오픈 사유 등을 영구 보존합니다.
 */

@Getter
@Setter
@NoArgsConstructor
public class ClosingAuditLog {

    private Long id;

    private ClosingCalendar closingCalendar;

    private ActionType actionType; // CALENDAR_OPEN, CALENDAR_CLOSED, PERIOD_REOPENED, TASK_COMPLETED, etc.

    private String previousStatus;

    private String currentStatus;

    private String actionReason; // 작업 사유 (특히 취소나 재오픈 시 필수)

    private String actionUser;

    private LocalDateTime actionAt;

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
