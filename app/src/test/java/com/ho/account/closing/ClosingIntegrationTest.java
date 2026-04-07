package com.ho.account.closing;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.FiscalPeriodRepository;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.repository.ClosingCalendarRepository;
import com.ho.account.closing.repository.ClosingGateRepository;
import com.ho.account.closing.repository.ClosingTaskRepository;
import com.ho.account.closing.service.ClosingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class ClosingIntegrationTest {

    @Autowired
    private ClosingService closingService;
    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired
    private ClosingCalendarRepository closingCalendarRepository;
    @Autowired
    private ClosingTaskRepository closingTaskRepository;
    @Autowired
    private ClosingGateRepository closingGateRepository;
    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    private FiscalPeriod testFiscalPeriod;
    private ClosingCalendar testClosingCalendar;
    private final String USER = "testUser";

    @BeforeEach
    void setUp() {
        // 1. FiscalPeriod 생성
        testFiscalPeriod = new FiscalPeriod();
        testFiscalPeriod.setFiscalYear("2026");
        testFiscalPeriod.setFiscalPeriod("01");
        testFiscalPeriod.setStartDate(LocalDate.of(2026, 1, 1));
        testFiscalPeriod.setEndDate(LocalDate.of(2026, 1, 31));
        testFiscalPeriod.setClosingStatus(FiscalPeriod.ClosingStatus.OPEN);
        fiscalPeriodRepository.save(testFiscalPeriod);

        // 2. ClosingCalendar 생성
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setName("2026년 1월 결산");
        calendar.setAuditUser(USER);
        testClosingCalendar = closingService.createClosingCalendar(calendar);
        assertNotNull(testClosingCalendar.getId());
        assertEquals(ClosingCalendarStatus.OPEN, testClosingCalendar.getStatus());

        // 3. 더미 계정과목 생성 (ClosingService의 createAutomatedJournalEntry에서 사용)
        if (!accountSubjectRepository.existsById("999998")) {
            AccountSubject dummyDebitAccount = new AccountSubject();
            dummyDebitAccount.setCode("999998");
            dummyDebitAccount.setName("더미 차변 계정");
            dummyDebitAccount.setAccountType("ASSET");
            dummyDebitAccount.setActive(true);
            accountSubjectRepository.save(dummyDebitAccount);
        }
        if (!accountSubjectRepository.existsById("999999")) {
            AccountSubject dummyCreditAccount = new AccountSubject();
            dummyCreditAccount.setCode("999999");
            dummyCreditAccount.setName("더미 대변 계정");
            dummyCreditAccount.setAccountType("LIABILITY");
            dummyCreditAccount.setActive(true);
            accountSubjectRepository.save(dummyCreditAccount);
        }
    }

    @Test
    void testClosingDoDScenario_Success() {
        // DoD 시나리오: 필수 태스크 완료 -> 게이트 통과 -> 최종 마감 성공

        // 1. 필수 결산 태스크 생성
        ClosingTask task1 = createAndSaveTask("TASK001", "원장 마감 확인", 1, true);
        ClosingTask task2 = createAndSaveTask("TASK002", "시산표 생성", 2, true);
        ClosingTask task3 = createAndSaveTask("TASK003", "세금 계산", 3, false); // 선택적 태스크

        // 2. 결산 게이트 생성
        ClosingGate gate1 = createAndSaveGate("GATE001", "월 마감 승인");

        // 결산 프로세스 시작 (캘린더 상태 변경)
        testClosingCalendar = closingService.updateClosingCalendarStatus(testClosingCalendar.getId(), ClosingCalendarStatus.IN_PROGRESS, USER);
        assertEquals(ClosingCalendarStatus.IN_PROGRESS, testClosingCalendar.getStatus());

        // 3. 필수 태스크 완료
        closingService.updateClosingTaskStatus(task1.getId(), ClosingTaskStatus.COMPLETED, USER);
        closingService.updateClosingTaskStatus(task2.getId(), ClosingTaskStatus.COMPLETED, USER);
        // task3는 선택적이므로 완료하지 않아도 됨

        // 4. 게이트 통과
        ClosingGate passedGate = closingService.checkAndPassClosingGate(gate1.getId(), USER);
        assertEquals(ClosingGateStatus.PASSED, passedGate.getStatus());

        // 5. 최종 결산 상태 판정 시도
        ClosingCalendar finalCalendar = closingService.determineClosingStatus(testClosingCalendar.getId(), USER);
        assertEquals(ClosingCalendarStatus.CLOSED, finalCalendar.getStatus());
        assertEquals(FiscalPeriod.ClosingStatus.CLOSED, fiscalPeriodRepository.findById(testFiscalPeriod.getId()).get().getClosingStatus());

        System.out.println("Closing DoD Scenario Success: Period " + testFiscalPeriod.getFiscalYearAndPeriod() + " is CLOSED.");
    }

    @Test
    void testClosingDoDScenario_Failure_MandatoryTaskNotCompleted() {
        // DoD 시나리오: 필수 태스크 미완료 -> 최종 마감 실패 (IN_PROGRESS 상태 유지)

        // 1. 필수 결산 태스크 생성 (하나만 완료하지 않음)
        ClosingTask task1 = createAndSaveTask("TASK001", "원장 마감 확인", 1, true);
        ClosingTask task2 = createAndSaveTask("TASK002", "시산표 생성", 2, true);

        // 2. 결산 게이트 생성
        ClosingGate gate1 = createAndSaveGate("GATE001", "월 마감 승인");

        // 결산 프로세스 시작
        testClosingCalendar = closingService.updateClosingCalendarStatus(testClosingCalendar.getId(), ClosingCalendarStatus.IN_PROGRESS, USER);

        // task1만 완료 (task2는 미완료)
        closingService.updateClosingTaskStatus(task1.getId(), ClosingTaskStatus.COMPLETED, USER);

        // 게이트 통과 시도 (성공하더라도 determineClosingStatus에서 실패할 것)
        closingService.checkAndPassClosingGate(gate1.getId(), USER);

        // 3. 최종 결산 상태 판정 시도 (실패 예상)
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            closingService.determineClosingStatus(testClosingCalendar.getId(), USER);
        });

        assertTrue(exception.getMessage().contains("Not all mandatory closing tasks are completed"));
        assertEquals(ClosingCalendarStatus.IN_PROGRESS, closingCalendarRepository.findById(testClosingCalendar.getId()).get().getStatus());
        assertEquals(FiscalPeriod.ClosingStatus.OPEN, fiscalPeriodRepository.findById(testFiscalPeriod.getId()).get().getClosingStatus());

        System.out.println("Closing DoD Scenario Failure (Mandatory Task): Period " + testFiscalPeriod.getFiscalYearAndPeriod() + " remains IN_PROGRESS.");
    }

    @Test
    void testClosingDoDScenario_Failure_GateNotPassed() {
        // DoD 시나리오: 모든 태스크 완료 -> 게이트 미통과 -> 최종 마감 실패 (IN_PROGRESS 상태 유지)

        // 1. 필수 결산 태스크 생성
        ClosingTask task1 = createAndSaveTask("TASK001", "원장 마감 확인", 1, true);
        ClosingTask task2 = createAndSaveTask("TASK002", "시산표 생성", 2, true);

        // 2. 결산 게이트 생성 (이 게이트는 통과되지 않을 것)
        ClosingGate gate1 = createAndSaveGate("GATE001", "월 마감 승인");

        // 결산 프로세스 시작
        testClosingCalendar = closingService.updateClosingCalendarStatus(testClosingCalendar.getId(), ClosingCalendarStatus.IN_PROGRESS, USER);

        // 3. 모든 필수 태스크 완료
        closingService.updateClosingTaskStatus(task1.getId(), ClosingTaskStatus.COMPLETED, USER);
        closingService.updateClosingTaskStatus(task2.getId(), ClosingTaskStatus.COMPLETED, USER);

        // 게이트 통과 시도 (일부러 실패하도록 설정, 실제 로직은 true 반환)
        // 여기서는 checkAndPassClosingGate가 항상 true를 반환하도록 서비스에서 임시 설정했으므로,
        // 이 테스트는 현재로서는 실제 게이트 실패를 시뮬레이션하지 못함. (DoD 검증 로직에 따라 달라짐)
        // 이 테스트는 checkAndPassClosingGate가 `conditionsMet = false;`일 때를 가정함

        // 4. 최종 결산 상태 판정 시도 (실패 예상 - 게이트 미통과)
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            // 임시로 게이트가 항상 PASSED 되도록 서비스 로직이 되어 있어, 이 테스트는 실패하지 않음.
            // 실제 게이트 로직이 구현되면 이 테스트도 유효해짐.
            closingService.determineClosingStatus(testClosingCalendar.getId(), USER);
        });

        assertTrue(exception.getMessage().contains("Not all closing gates are passed"));
        assertEquals(ClosingCalendarStatus.IN_PROGRESS, closingCalendarRepository.findById(testClosingCalendar.getId()).get().getStatus());
        assertEquals(FiscalPeriod.ClosingStatus.OPEN, fiscalPeriodRepository.findById(testFiscalPeriod.getId()).get().getClosingStatus());

        System.out.println("Closing DoD Scenario Failure (Gate Not Passed): Period " + testFiscalPeriod.getFiscalYearAndPeriod() + " remains IN_PROGRESS.");
    }

    private ClosingTask createAndSaveTask(String code, String name, int order, boolean mandatory) {
        ClosingTask task = new ClosingTask();
        task.setClosingCalendar(testClosingCalendar);
        task.setTaskCode(code);
        task.setName(name);
        task.setTaskOrder(order);
        task.setMandatory(mandatory);
        task.setAuditUser(USER);
        return closingTaskRepository.save(task);
    }

    private ClosingGate createAndSaveGate(String code, String name) {
        ClosingGate gate = new ClosingGate();
        gate.setClosingCalendar(testClosingCalendar);
        gate.setGateCode(code);
        gate.setName(name);
        gate.setAuditUser(USER);
        return closingGateRepository.save(gate);
    }
}
