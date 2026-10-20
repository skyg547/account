package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ClosingAuditLog;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of ClosingAuditLog; business transitions live in the domain aggregate. */
@Entity(name = "ClosingAuditLog")
@Table(name = "closing_audit_logs")
@Getter
@Setter
public class ClosingAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ClosingCalendarEntity closingCalendar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClosingAuditLog.ActionType actionType; // CALENDAR_OPEN, CALENDAR_CLOSED, PERIOD_REOPENED, TASK_COMPLETED, etc.

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

    @PrePersist
    protected void onCreate() {
        this.actionAt = LocalDateTime.now();
    }
}
