package com.ho.account.journal.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Customer;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.CustomerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.closing.service.ClosingService;
import com.ho.account.unsettled.service.UnsettledService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final CustomerRepository customerRepository;
    private final ClosingService closingService;
    private final UnsettledService unsettledService;

    @Autowired
    public JournalService(JournalEntryRepository journalEntryRepository,
                          AccountSubjectRepository accountSubjectRepository,
                          DepartmentRepository departmentRepository,
                          CustomerRepository customerRepository,
                          ClosingService closingService,
                          UnsettledService unsettledService) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.customerRepository = customerRepository;
        this.closingService = closingService;
        this.unsettledService = unsettledService;
    }

    // 전표 생성
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        if (journalEntry.getAccountingDate() == null) {
            journalEntry.setAccountingDate(journalEntry.getSlipDate());
        }

        if (closingService.isClosed(journalEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 생성할 수 없습니다.");
        }

        validateJournalEntry(journalEntry);

        String slipNo = generateSlipNo(journalEntry.getAccountingDate());
        journalEntry.setSlipNo(slipNo);
        journalEntry.setStatus("DRAFT");

        for (JournalDetail detail : journalEntry.getDetails()) {
            detail.setJournalEntry(journalEntry);
        }

        return journalEntryRepository.save(journalEntry);
    }

    // 전표 승인
    public void approveJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (!"REQUESTED".equals(entry.getStatus())) {
            throw new IllegalStateException("승인 요청된 전표만 승인할 수 있습니다.");
        }

        entry.setStatus("APPROVED");
        entry.setRejectionReason(null);
        journalEntryRepository.save(entry);

        for (JournalDetail detail : entry.getDetails()) {
            if (Boolean.TRUE.equals(detail.getAccountSubject().getManageUnsettled())) {
                // UnsettledService의 메서드 시그니처가 변경될 예정이므로 임시 주석 처리 또는 수정 필요
                // 현재는 JournalDetail 객체를 그대로 넘기는 구조라고 가정
                 unsettledService.createUnsettledItem(detail); 
            }
        }
    }

    // 전표 수정
    public JournalEntry updateJournalEntry(Long id, JournalEntry journalEntryDetails) {
        JournalEntry existingEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(existingEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 수정할 수 없습니다.");
        }
        if (journalEntryDetails.getAccountingDate() != null && closingService.isClosed(journalEntryDetails.getAccountingDate())) {
            throw new IllegalStateException("변경하려는 날짜의 월은 이미 마감되었습니다.");
        }

        if (!"DRAFT".equals(existingEntry.getStatus()) && !"REJECTED".equals(existingEntry.getStatus())) {
            throw new IllegalStateException("작성중이거나 반려된 전표만 수정할 수 있습니다.");
        }

        existingEntry.setSlipDate(journalEntryDetails.getSlipDate());
        existingEntry.setAccountingDate(journalEntryDetails.getAccountingDate() != null ? 
                                      journalEntryDetails.getAccountingDate() : journalEntryDetails.getSlipDate());
        existingEntry.setDescription(journalEntryDetails.getDescription());

        existingEntry.clearDetails();
        for (JournalDetail detail : journalEntryDetails.getDetails()) {
            existingEntry.addDetail(detail);
        }

        validateJournalEntry(existingEntry);

        return journalEntryRepository.save(existingEntry);
    }

    // 전표 삭제
    public void deleteJournalEntry(Long id) {
        JournalEntry existingEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(existingEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 삭제할 수 없습니다.");
        }

        if (!"DRAFT".equals(existingEntry.getStatus())) {
            throw new IllegalStateException("작성중인 전표만 삭제할 수 있습니다.");
        }
        journalEntryRepository.delete(existingEntry);
    }

    // 승인 요청
    public void requestApproval(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));
        
        if (closingService.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (!"DRAFT".equals(entry.getStatus()) && !"REJECTED".equals(entry.getStatus())) {
             throw new IllegalStateException("작성중이거나 반려된 전표만 승인 요청할 수 있습니다.");
        }
        
        entry.setStatus("REQUESTED");
        journalEntryRepository.save(entry);
    }

    // 전표 반려
    public void rejectJournalEntry(Long id, String reason) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (!"REQUESTED".equals(entry.getStatus())) {
            throw new IllegalStateException("승인 요청된 전표만 반려할 수 있습니다.");
        }

        entry.setStatus("REJECTED");
        entry.setRejectionReason(reason);
        journalEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalEntryRepository.findBySlipNo(slipNo);
    }

    private void validateJournalEntry(JournalEntry entry) {
        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;

        for (JournalDetail detail : entry.getDetails()) {
            if (detail.getAccountSubject() == null || detail.getAccountSubject().getAccountCode() == null) {
                throw new IllegalArgumentException("계정과목 코드는 필수입니다.");
            }
            AccountSubject account = accountSubjectRepository.findById(detail.getAccountSubject().getAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정과목입니다: " + detail.getAccountSubject().getAccountCode()));
            if (!account.getUseYn()) {
                throw new IllegalArgumentException("사용 중지된 계정과목입니다: " + account.getAccountName());
            }
            detail.setAccountSubject(account);

            if (detail.getDepartment() != null && detail.getDepartment().getDeptCode() != null) {
                Department dept = departmentRepository.findByDeptCode(detail.getDepartment().getDeptCode())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다: " + detail.getDepartment().getDeptCode()));
                if (!dept.getUseYn()) {
                    throw new IllegalArgumentException("사용 중지된 부서입니다: " + dept.getDeptName());
                }
                detail.setDepartment(dept);
            }

            if (detail.getCustomer() != null && detail.getCustomer().getCustomerCode() != null) {
                Customer customer = customerRepository.findByCustomerCode(detail.getCustomer().getCustomerCode())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래처입니다: " + detail.getCustomer().getCustomerCode()));
                if (!customer.getUseYn()) {
                    throw new IllegalArgumentException("사용 중지된 거래처입니다: " + customer.getCustomerName());
                }
                detail.setCustomer(customer);
            }

            if ("DEBIT".equals(detail.getDrcrType())) {
                debitSum = debitSum.add(detail.getAmount());
            } else if ("CREDIT".equals(detail.getDrcrType())) {
                creditSum = creditSum.add(detail.getAmount());
            } else {
                throw new IllegalArgumentException("차대 구분은 DEBIT 또는 CREDIT이어야 합니다.");
            }
        }

        if (debitSum.compareTo(creditSum) != 0) {
            throw new IllegalArgumentException("차변과 대변의 합계가 일치하지 않습니다. 차변: " + debitSum + ", 대변: " + creditSum);
        }
    }

    private String generateSlipNo(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<JournalEntry> entries = journalEntryRepository.findByAccountingDate(date);
        int seq = entries.size() + 1;
        return String.format("%s-%03d", dateStr, seq);
    }
}
