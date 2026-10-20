package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.domain.EodState;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of DailyClosingStatus; business transitions live in the domain aggregate. */
@Entity(name = "DailyClosingStatus")
@Table(name = "daily_closing_status")
@Getter
@Setter
public class DailyClosingStatusEntity {

    @Id
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private EodState state;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "prepared_by", length = 80)
    private String preparedBy;

    @Column(name = "closing_started_at")
    private LocalDateTime closingStartedAt;

    @Column(name = "closing_started_by", length = 80)
    private String closingStartedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @Column(name = "bod_started_at")
    private LocalDateTime bodStartedAt;

    @Column(name = "bod_started_by", length = 80)
    private String bodStartedBy;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "opened_by", length = 80)
    private String openedBy;

}
