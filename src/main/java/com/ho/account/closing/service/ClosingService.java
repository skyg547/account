package com.ho.account.closing.service;

import com.ho.account.closing.domain.ClosingPeriod;
import com.ho.account.closing.domain.ClosingStatusEnum;
import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.repository.ClosingPeriodRepository;
import com.ho.account.closing.repository.DailyClosingStatusRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import com.ho.account.closing.domain.PeriodType;
import com.ho.account.closing.domain.ApprovalStatus;
import java.time.LocalDateTime;

import java.util.List;

@Service
@Transactional
public class ClosingService {

    private final ClosingPeriodRepository closingPeriodRepository;
    private final DailyClosingStatusRepository dailyClosingStatusRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Autowired
    public ClosingService(ClosingPeriodRepository closingPeriodRepository,
                          DailyClosingStatusRepository dailyClosingStatusRepository,
                          JournalEntryRepository journalEntryRepository) {
        this.closingPeriodRepository = closingPeriodRepository;
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
    public ClosingPeriod closeMonth(Long yearMonthId, String userId) {
        ClosingPeriod closingPeriod = closingPeriodRepository.findById(yearMonthId).orElse(null);

        if (closingPeriod != null && closingPeriod.getStatus() == ClosingStatusEnum.CLOSED) {
            throw new IllegalStateException("이미 마감된 월입니다: " + yearMonthId);
        }

        // Determine start and end dates for the month
        YearMonth ym = YearMonth.parse(String.valueOf(yearMonthId), DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        if (closingPeriod == null) {
            // Create a new closing period if it doesn't exist
            closingPeriod = new ClosingPeriod(yearMonthId, PeriodType.MONTHLY, startDate, endDate, userId);
        }

        // Pre-check for unapproved journal entries
        List<JournalEntry> entries = journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
        boolean hasUnapproved = entries.stream()
                .anyMatch(e -> !"APPROVED".equals(e.getStatus()));

        if (hasUnapproved) {
            closingPeriod.setStatus(ClosingStatusEnum.FAILED); // Mark as failed due to unapproved entries
            closingPeriodRepository.save(closingPeriod);
            throw new IllegalStateException("미승인 전표가 존재하여 마감할 수 없습니다. 마감 상태: FAILED");
        }

        // Transition status to IN_PROGRESS, then CLOSED (simplified for now, will involve tasks later)
        closingPeriod.setStatus(ClosingStatusEnum.IN_PROGRESS);
        closingPeriod.setClosedBy(userId); // Use closedBy for the person initiating the closing
        closingPeriodRepository.save(closingPeriod);

        // For now, directly setting to CLOSED. This will be replaced by task completion logic.
        closingPeriod.setStatus(ClosingStatusEnum.CLOSED);
        closingPeriodRepository.save(closingPeriod);

        return closingPeriod;
    }

    // 월 마감 취소 (재오픈 요청)
    public ClosingPeriod requestMonthReopen(Long yearMonthId, String requestorId, String reason) {
        ClosingPeriod closingPeriod = closingPeriodRepository.findById(yearMonthId)
                .orElseThrow(() -> new IllegalArgumentException("마감 정보를 찾을 수 없습니다: " + yearMonthId));

        if (closingPeriod.getStatus() != ClosingStatusEnum.CLOSED) {
            throw new IllegalStateException("마감되지 않았거나 이미 재오픈 처리된 월입니다.");
        }

        // Set status to PENDING_APPROVAL for re-opening
        closingPeriod.setStatus(ClosingStatusEnum.PENDING_APPROVAL);
        closingPeriod.setReopenReason(reason);
        closingPeriod.setReopenedBy(requestorId);
        closingPeriod.setApprovalStatus(ApprovalStatus.PENDING); // Set approval status
        closingPeriodRepository.save(closingPeriod);

        return closingPeriod;
    }

    // 월 마감 재오픈 승인
    public ClosingPeriod approveMonthReopen(Long yearMonthId, String approverId) {
        ClosingPeriod closingPeriod = closingPeriodRepository.findById(yearMonthId)
                .orElseThrow(() -> new IllegalArgumentException("마감 정보를 찾을 수 없습니다: " + yearMonthId));

        if (closingPeriod.getStatus() != ClosingStatusEnum.PENDING_APPROVAL) {
            throw new IllegalStateException("재오픈 승인 대기 상태가 아닙니다.");
        }
        if (closingPeriod.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("재오픈 승인 대기 상태가 아닙니다.");
        }

        closingPeriod.setStatus(ClosingStatusEnum.REOPENED);
        closingPeriod.setApprovalStatus(ApprovalStatus.APPROVED);
        closingPeriod.setApprover(approverId);
        closingPeriod.setApprovalDate(LocalDateTime.now());
        closingPeriodRepository.save(closingPeriod);

        return closingPeriod;
    }

    // 월 마감 재오픈 반려
    public ClosingPeriod rejectMonthReopen(Long yearMonthId, String approverId) {
        ClosingPeriod closingPeriod = closingPeriodRepository.findById(yearMonthId)
                .orElseThrow(() -> new IllegalArgumentException("마감 정보를 찾을 수 없습니다: " + yearMonthId));

        if (closingPeriod.getStatus() != ClosingStatusEnum.PENDING_APPROVAL) {
            throw new IllegalStateException("재오픈 승인 대기 상태가 아닙니다.");
        }
        if (closingPeriod.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("재오픈 승인 대기 상태가 아닙니다.");
        }

        closingPeriod.setApprovalStatus(ApprovalStatus.REJECTED);
        closingPeriod.setApprover(approverId);
        closingPeriod.setApprovalDate(LocalDateTime.now());
        // If rejected, revert to CLOSED status
        closingPeriod.setStatus(ClosingStatusEnum.CLOSED);
        closingPeriodRepository.save(closingPeriod);

        return closingPeriod;
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
        Long yearMonthId = Long.parseLong(date.format(DateTimeFormatter.ofPattern("yyyyMM")));
        return isMonthClosed(yearMonthId);
    }

    // 월 마감 여부 확인 (YYYYMM 기준)
    @Transactional(readOnly = true)
    public boolean isMonthClosed(Long yearMonthId) {
        return closingPeriodRepository.findById(yearMonthId)
                .map(period -> period.getStatus() == ClosingStatusEnum.CLOSED)
                .orElse(false);
    }
}
