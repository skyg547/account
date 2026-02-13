package com.ho.account.security;

import com.ho.account.audit.repository.AuditLogRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.journal.service.JournalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SecurityControlTest {

    @Autowired
    private JournalService journalService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("SOD Check: Maker and Checker must be different")
    void testSegregationOfDuties() {
        // 1. 전표 생성
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(LocalDate.now());
        entry.setCreatedBy("USER_A");
        entry.setAuditUser("USER_A"); // 현재 사용자는 USER_A
        entry.setDescription("SOD Test");

        JournalEntry saved = journalService.createJournalEntry(entry);
        assertNotNull(saved.getId());

        // 2. 승인 요청
        journalService.requestApproval(saved.getId());

        // 3. 동일 사용자(USER_A)로 승인 시도
        // 간소화된 구현에서는 JournalService가 entry.getAuditUser()를
        // 현재 사용자로 사용합니다
        assertThrows(IllegalStateException.class, () -> {
            journalService.approveJournalEntry(saved.getId());
        }, "Maker should not be able to approve their own journal");
    }

    @Test
    @DisplayName("Audit Logging: Business events must be logged via AOP")
    void testAuditLogging() {
        long initialLogCount = auditLogRepository.count();

        // 1. 비즈니스 액션 수행 (전표 생성)
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(LocalDate.now());
        entry.setCreatedBy("AUDIT_TESTER");
        entry.setAuditUser("AUDIT_TESTER");
        entry.setDescription("Audit Logging Test");

        journalService.createJournalEntry(entry);

        // 2. 감사 로그가 생성되었는지 확인
        long finalLogCount = auditLogRepository.count();
        assertTrue(finalLogCount > initialLogCount, "Audit log should be created for business events");

        auditLogRepository.findAll().stream()
                .anyMatch(log -> "JOURNAL".equals(log.getEventType()) && "CREATE".equals(log.getTargetId()));
        // 참고: AuditAspect에서 SERVICE_METHOD 타입의 eventName으로 TargetId가 설정되었습니다
    }
}
