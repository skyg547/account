package com.ho.account.report.service;

import com.ho.account.report.domain.FinancialNote;
import com.ho.account.report.domain.FinancialReport;
import com.ho.account.report.repository.FinancialNoteRepository;
import com.ho.account.report.repository.FinancialReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주석 및 보고서 관리를 위한 서비스 클래스입니다.
 * 기본적인 CRUD(Create, Read, Update, Delete) 기능을 제공합니다.
 */
@Service
@Transactional
public class ReportService {

    private final FinancialNoteRepository noteRepository;
    private final FinancialReportRepository reportRepository;

    @Autowired
    public ReportService(FinancialNoteRepository noteRepository, FinancialReportRepository reportRepository) {
        this.noteRepository = noteRepository;
        this.reportRepository = reportRepository;
    }

    // 주석 관련
    public FinancialNote saveNote(FinancialNote note) {
        return noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<FinancialNote> findNotesByYearMonth(String yearMonth) {
        return noteRepository.findByYearMonth(yearMonth);
    }

    // 보고서 관련
    public FinancialReport saveReport(FinancialReport report) {
        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<FinancialReport> findAllReports() {
        return reportRepository.findAll();
    }
}
