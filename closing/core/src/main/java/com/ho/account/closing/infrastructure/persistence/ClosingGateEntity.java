package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ClosingGate;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of ClosingGate; business transitions live in the domain aggregate. */
@Entity(name = "ClosingGate")
@Table(name = "closing_gates")
@Getter
@Setter
public class ClosingGateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ClosingCalendarEntity closingCalendar;

    @Column(nullable = false, length = 100)
    private String name; // 게이트명 (예: "PRE-CLOSING 완료", "조정 전표 검토 완료")

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingGate.ClosingGateStatus status; // PENDING, PASSED, FAILED

    // 게이트 통과 조건(JSON). 예: 모든 필수 ClosingTask가 완료 상태인지 확인하는 조건.
    @Column(columnDefinition = "TEXT")
    private String checkConditionJson;

    @Column(length = 50)
    private String passedBy;

    private LocalDateTime passedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @Transient
    private String gateCode;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ClosingGate.ClosingGateStatus.PENDING;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
