package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_closing_status")
public class DailyClosingStatus {

    @Id
    private LocalDate date; // YYYY-MM-DD

    @Column(nullable = false)
    private Boolean isClosed = false;

    private LocalDateTime closedAt;
    private String closedBy;

    public DailyClosingStatus() {}

    public DailyClosingStatus(LocalDate date, Boolean isClosed, String closedBy) {
        this.date = date;
        this.isClosed = isClosed;
        this.closedBy = closedBy;
        if (isClosed) {
            this.closedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

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
