package com.ho.account.report.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 재무제표 주석(Financial Note) 정보를 담는 엔티티 클래스입니다.
 * 주석은 재무제표의 수치만으로는 알 수 없는 추가적인 정보나 회계 정책 등을 설명합니다.
 */
@Entity
@Table(name = "financial_notes")
public class FinancialNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String yearMonth; // 대상 연월 (YYYYMM)

    @Column(nullable = false, length = 50)
    private String noteCategory; // 주석 분류 (예: 회계정책, 우발부채, 특수관계자거래)

    @Column(columnDefinition = "TEXT")
    private String content; // 주석 내용 (긴 텍스트)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

    public String getNoteCategory() { return noteCategory; }
    public void setNoteCategory(String noteCategory) { this.noteCategory = noteCategory; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
