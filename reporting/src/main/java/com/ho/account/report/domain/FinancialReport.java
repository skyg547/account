package com.ho.account.report.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대외 보고서(Financial Report) 정보를 관리하는 엔티티입니다.
 * 사업보고서, 감사보고서 등 공시용 파일 정보를 저장합니다.
 */
@Entity
@Table(name = "financial_reports")
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String reportName; // 보고서 명

    @Column(nullable = false)
    private LocalDate reportDate; // 보고서 기준일

    @Column(nullable = false)
    private String filePath; // 파일 저장 경로

    @Column(nullable = false)
    private String fileType; // 파일 종류 (PDF, DOCX 등)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
