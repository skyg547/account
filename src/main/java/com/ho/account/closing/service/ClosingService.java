package com.ho.account.closing.service;

import com.ho.account.closing.domain.ClosingStatus;
import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.repository.ClosingStatusRepository;
import com.ho.account.closing.repository.DailyClosingStatusRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ClosingService {

    private final ClosingStatusRepository closingStatusRepository;
    private final DailyClosingStatusRepository dailyClosingStatusRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Autowired
    public ClosingService(ClosingStatusRepository closingStatusRepository,
                          DailyClosingStatusRepository dailyClosingStatusRepository,
                          JournalEntryRepository journalEntryRepository) {
        this.closingStatusRepository = closingStatusRepository;
        this.dailyClosingStatusRepository = dailyClosingStatusRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    // 일 마감 실행
    public void closeDay(LocalDate date, String userId) {
        // 1. 이미 마감되었는지 확인
        if (isDayClosed(date)) {
            throw new IllegalStateException("이미 마감된 일자입니다: " + date);
        }

        // 2. 미승인 전표 확인 (해당 일자)
        List<JournalEntry> entries = journalEntryRepository.findByAccountingDate(date);
        boolean hasUnapproved = entries.stream()
                .anyMatch(e -> !"APPROVED".equals(e.getStatus()));

        if (hasUnapproved) {
            throw new IllegalStateException("미승인 전표가 존재하여 마감할 수 없습니다.");
        }

        // 3. 마감 처리
        DailyClosingStatus status = dailyClosingStatusRepository.findByDate(date)
                .orElse(new DailyClosingStatus(date, false, null));
        
        status.setIsClosed(true);
        status.setClosedBy(userId);
        dailyClosingStatusRepository.save(status);
    }

    // 일 마감 취소
    public void cancelDayClosing(LocalDate date) {
        // 월 마감 여부 확인 (월 마감이 되어있으면 일 마감 취소 불가)
        if (isMonthClosed(date)) {
            throw new IllegalStateException("월 마감이 완료된 상태에서는 일 마감을 취소할 수 없습니다.");
        }

        DailyClosingStatus status = dailyClosingStatusRepository.findByDate(date)
                .orElseThrow(() -> new IllegalArgumentException("마감 정보를 찾을 수 없습니다: " + date));

        if (!status.getIsClosed()) {
            throw new IllegalStateException("마감되지 않은 일자입니다.");
        }

        status.setIsClosed(false);
        dailyClosingStatusRepository.save(status);
    }

    // 월 마감 실행
    public void closeMonth(String yearMonth, String userId) {
        if (isMonthClosed(yearMonth)) {
            throw new IllegalStateException("이미 마감된 월입니다: " + yearMonth);
        }

        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 해당 월의 모든 일자가 마감되었는지 확인 (선택 사항: 보통 월 마감 시 일괄 마감 처리하거나 체크함)
        // 여기서는 미승인 전표만 체크
        List<JournalEntry> entries = journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
        boolean hasUnapproved = entries.stream()
                .anyMatch(e -> !"APPROVED".equals(e.getStatus()));

        if (hasUnapproved) {
            throw new IllegalStateException("미승인 전표가 존재하여 마감할 수 없습니다.");
        }

        ClosingStatus status = closingStatusRepository.findByYearMonth(yearMonth)
                .orElse(new ClosingStatus(yearMonth, false, null));
        
        status.setIsClosed(true);
        status.setClosedBy(userId);
        closingStatusRepository.save(status);
    }

    // 월 마감 취소
    public void cancelMonthClosing(String yearMonth) {
        ClosingStatus status = closingStatusRepository.findByYearMonth(yearMonth)
                .orElseThrow(() -> new IllegalArgumentException("마감 정보를 찾을 수 없습니다: " + yearMonth));

        if (!status.getIsClosed()) {
            throw new IllegalStateException("마감되지 않은 월입니다.");
        }

        status.setIsClosed(false);
        closingStatusRepository.save(status);
    }

    // 통합 마감 여부 확인 (일 마감 OR 월 마감)
    @Transactional(readOnly = true)
    public boolean isClosed(LocalDate date) {
        return isDayClosed(date) || isMonthClosed(date);
    }

    // 일 마감 여부 확인
    @Transactional(readOnly = true)
    public boolean isDayClosed(LocalDate date) {
        return dailyClosingStatusRepository.findByDate(date)
                .map(DailyClosingStatus::getIsClosed)
                .orElse(false);
    }

    // 월 마감 여부 확인 (날짜 기준)
    @Transactional(readOnly = true)
    public boolean isMonthClosed(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        return isMonthClosed(yearMonth);
    }

    // 월 마감 여부 확인 (YYYYMM 기준)
    @Transactional(readOnly = true)
    public boolean isMonthClosed(String yearMonth) {
        return closingStatusRepository.findByYearMonth(yearMonth)
                .map(ClosingStatus::getIsClosed)
                .orElse(false);
    }
}
