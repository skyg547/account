package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.ReportLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "rpt_snapshot_detail")
class ReportSnapshotDetailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_header_id", nullable = false)
    private ReportSnapshotHeaderJpaEntity header;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "line_code", nullable = false, length = 80)
    private String lineCode;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "previous_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal previousAmount;

    @Column(name = "note_number", length = 40)
    private String noteNumber;

    @Column(name = "line_level", nullable = false)
    private int lineLevel;

    protected ReportSnapshotDetailJpaEntity() {
    }

    static ReportSnapshotDetailJpaEntity from(ReportLine line, int lineOrder) {
        ReportSnapshotDetailJpaEntity detail = new ReportSnapshotDetailJpaEntity();
        detail.lineOrder = lineOrder;
        detail.lineCode = line.getLineCode();
        detail.label = line.getLabel();
        detail.currentAmount = line.getCurrentAmount();
        detail.previousAmount = line.getPreviousAmount();
        detail.noteNumber = line.getNoteNumber();
        detail.lineLevel = line.getLevel();
        return detail;
    }

    void assignHeader(ReportSnapshotHeaderJpaEntity header) {
        this.header = header;
    }

    ReportLine toDomain() {
        return new ReportLine(
                lineCode,
                label,
                currentAmount,
                previousAmount,
                noteNumber,
                lineLevel);
    }
}
