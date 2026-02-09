package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 재무제표 주석(Financial Note) 정보를 담는 엔티티 클래스입니다.
 * 주석은 재무제표의 수치만으로는 알 수 없는 추가적인 정보나 회계 정책 등을 설명합니다.
 */
@Entity
@Table(name = "closing_status")
public class ClosingStatus {

    @Id
    @Column(length = 6)
    private String yearMonth; // YYYYMM

    @Column(nullable = false)
    private Boolean isClosed = false;

    private LocalDateTime closedAt;
    private String closedBy;

    public ClosingStatus() {}

    public ClosingStatus(String yearMonth, Boolean isClosed, String closedBy) {
        this.yearMonth = yearMonth;
        this.isClosed = isClosed;
        this.closedBy = closedBy;
        if (isClosed) {
            this.closedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean closed) {
        isClosed = closed;
        if (closed) {
            this.closedAt = LocalDateTime.now();
        } else {
            this.closedAt = null;
            this.closedBy = null;
        }
    }

    public LocalDateTime getClosedAt() { return closedAt; }
    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
}
