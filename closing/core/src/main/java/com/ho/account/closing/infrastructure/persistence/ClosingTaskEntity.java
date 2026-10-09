package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ClosingTask;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of ClosingTask; business transitions live in the domain aggregate. */
@Entity(name = "ClosingTask")
@Table(name = "closing_tasks")
@Getter
@Setter
public class ClosingTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ClosingCalendarEntity closingCalendar;

    @Column(nullable = false, length = 200)
    private String name; // 태스크명 (예: "은행잔고 대조 완료", "외화 평가 실행")

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ClosingTask.ClosingTaskCategory category; // PRE_CLOSING, CLOSING_ENTRY, POST_CLOSING

    private LocalDateTime dueDate; // 태스크 완료 기한

    @Column(length = 50)
    private String assignedTo; // 담당자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingTask.ClosingTaskStatus status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    // 태스크 완료 조건(JSON). 예: 특정 대사 실행이 SUCCESS 상태인지 확인하는 조건.
    @Column(columnDefinition = "TEXT")
    private String completionConditionJson;

    @Column(nullable = false)
    private boolean isMandatory; // 필수 태스크 여부

    @Column(nullable = false)
    private Integer taskOrder; // 태스크 실행 순서

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @Transient
    private String taskCode;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ClosingTask.ClosingTaskStatus.PENDING;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
