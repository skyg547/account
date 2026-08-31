package com.ho.account.expenditure.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionCommand;
import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.Budget;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 지출결의 생성/수정/반려 시 예산 통제(Budget Control) 연동 검증 통합 테스트
 */
class ExpenditureResolutionBudgetIntegrationTest {

    private BudgetPersistencePort budgetPersistencePort;
    private ExpenditureResolutionPersistencePort resolutionPersistencePort;
    private MasterDataQueryPort masterDataQueryPort;
    private JournalPostingPort journalPostingPort;
    private AssetRegistrationPort assetRegistrationPort;
    private TaxInvoiceQueryPort taxInvoiceQueryPort;

    private BudgetService budgetService;
    private ExpenditureResolutionService resolutionService;

    @BeforeEach
    void setUp() {
        budgetPersistencePort = new InMemoryBudgetPersistencePort();
        resolutionPersistencePort = new InMemoryExpenditureResolutionPersistencePort();
        budgetService = new BudgetService(budgetPersistencePort);

        masterDataQueryPort = new MasterDataQueryPort() {
            @Override
            public Optional<DepartmentRef> findDepartment(String departmentCode) {
                return Optional.of(new DepartmentRef(departmentCode, "Department " + departmentCode, "COST_CENTER"));
            }

            @Override
            public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
                return Optional.of(new AccountSubjectRef(accountCode, "Account " + accountCode, false, false));
            }

            @Override
            public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
                return Optional.of(new BusinessPartnerRef(businessPartnerCode, "Partner " + businessPartnerCode, "VENDOR", true));
            }
        };

        journalPostingPort = new JournalPostingPort() {
            private final AtomicLong seq = new AtomicLong(1);
            @Override
            public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
                long id = seq.getAndIncrement();
                return new JournalPostingResult(id, "SLIP-" + id, "DRAFT");
            }

            @Override
            public void approveAndPost(Long journalEntryId, String actor) {}
        };

        assetRegistrationPort = new AssetRegistrationPort() {
            @Override
            public void registerAcquiredAsset(com.ho.account.contracts.asset.AssetAcquisitionCommand command) {}

            @Override
            public void activateLeaseContract(Long leaseContractId) {}
        };

        taxInvoiceQueryPort = taxInvoiceId -> Optional.empty();

        resolutionService = new ExpenditureResolutionService(
                resolutionPersistencePort,
                journalPostingPort,
                masterDataQueryPort,
                budgetService,
                assetRegistrationPort,
                taxInvoiceQueryPort
        );
    }

    @Test
    void expenditureResolutionCreationSucceedsWhenBudgetIsSufficient() {
        // 1. 예산 50,000 배정
        budgetService.assignBudget("202604", "D001", "EXP001", new BigDecimal("50000.00"));

        // 2. 30,000 지출 결의 생성
        ExpenditureResolutionCommand command = new ExpenditureResolutionCommand(
                "비품 구매",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                null,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP001",
                        new BigDecimal("30000.00"),
                        "BP001",
                        "모니터 구매"
                ))
        );

        ExpenditureResolution resolution = resolutionService.createResolution(command);

        assertNotNull(resolution);
        assertEquals(ExpenditureResolutionStatus.DRAFT, resolution.getStatus());
        assertEquals(0, new BigDecimal("30000.00").compareTo(resolution.getTotalAmount()));

        // 잔여 예산 20,000 확인
        assertEquals(0, new BigDecimal("20000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));
    }

    @Test
    void expenditureResolutionCreationFailsWhenBudgetIsExceeded() {
        // 1. 예산 10,000 배정
        budgetService.assignBudget("202604", "D001", "EXP001", new BigDecimal("10000.00"));

        // 2. 15,000 지출 결의 생성 시도 -> 한도 초과 예외 발생
        ExpenditureResolutionCommand command = new ExpenditureResolutionCommand(
                "서버 구매",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                null,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP001",
                        new BigDecimal("15000.00"),
                        "BP001",
                        "서버 장비"
                ))
        );

        assertThrows(IllegalStateException.class, () -> resolutionService.createResolution(command));

        // 예산은 차감되지 않고 10,000 유지
        assertEquals(0, new BigDecimal("10000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));
    }

    @Test
    void expenditureResolutionUpdateRestoresOldBudgetAndDeductsNewBudget() {
        // 1. 예산 50,000 배정 후 20,000 결의 생성
        budgetService.assignBudget("202604", "D001", "EXP001", new BigDecimal("50000.00"));

        ExpenditureResolutionCommand command = new ExpenditureResolutionCommand(
                "사무용품",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                null,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP001",
                        new BigDecimal("20000.00"),
                        "BP001",
                        "1차 구매"
                ))
        );
        ExpenditureResolution resolution = resolutionService.createResolution(command);
        assertEquals(0, new BigDecimal("30000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));

        // 2. 결의서 수정: 35,000으로 증액 수정 (기존 20,000 복원 -> 50,000 -> 35,000 차감 -> 잔여 15,000)
        ExpenditureResolutionCommand updateCommand = new ExpenditureResolutionCommand(
                "사무용품 수정",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                null,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP001",
                        new BigDecimal("35000.00"),
                        "BP001",
                        "수정 구매"
                ))
        );
        resolutionService.updateResolution(resolution.getId(), updateCommand);

        assertEquals(0, new BigDecimal("15000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));
    }

    @Test
    void rejectionRestoresAllocatedBudget() {
        // 1. 예산 50,000 배정 후 20,000 결의 생성
        budgetService.assignBudget("202604", "D001", "EXP001", new BigDecimal("50000.00"));

        ExpenditureResolutionCommand command = new ExpenditureResolutionCommand(
                "사무용품",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                null,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP001",
                        new BigDecimal("20000.00"),
                        "BP001",
                        "구매"
                ))
        );
        ExpenditureResolution resolution = resolutionService.createResolution(command);
        resolutionService.requestApproval(resolution.getId());

        assertEquals(0, new BigDecimal("30000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));

        // 2. 반려 처리 시 예산 전액 복원
        resolutionService.rejectResolution(resolution.getId(), "예산 집행 보류");

        assertEquals(0, new BigDecimal("50000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));
    }

    @Test
    void expenditureResolutionCreationSucceedsWithoutOrphanFailureWhenNoBudgetConfigured() {
        // 예산이 별도로 설정되지 않은 상태에서도 default fallback으로 인해 결의 생성이 차단되지 않음
        ExpenditureResolutionCommand command = new ExpenditureResolutionCommand(
                "자유 지출",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                null,
                List.of(new ExpenditureResolutionCommand.DetailCommand(
                        "EXP999",
                        new BigDecimal("100000.00"),
                        "BP001",
                        "예산 미지정 항목"
                ))
        );

        ExpenditureResolution resolution = resolutionService.createResolution(command);
        assertNotNull(resolution);
        assertEquals(ExpenditureResolutionStatus.DRAFT, resolution.getStatus());
    }

    static class InMemoryBudgetPersistencePort implements BudgetPersistencePort {
        private final Map<String, Budget> store = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public Optional<Budget> findByYearMonthAndDepartmentCodeAndAccountCode(
                String yearMonth, String departmentCode, String accountCode) {
            String key = yearMonth + "-" + departmentCode + "-" + accountCode;
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public Budget save(Budget budget) {
            if (budget.getId() == null) {
                budget.setId(seq.getAndIncrement());
            }
            String key = budget.getYearMonth() + "-" + budget.getDeptCode() + "-" + budget.getAccountCode();
            store.put(key, budget);
            return budget;
        }
    }

    static class InMemoryExpenditureResolutionPersistencePort implements ExpenditureResolutionPersistencePort {
        private final Map<Long, ExpenditureResolution> store = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public ExpenditureResolution save(ExpenditureResolution resolution) {
            if (resolution.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(resolution, "id", seq.getAndIncrement());
            }
            store.put(resolution.getId(), resolution);
            return resolution;
        }

        @Override
        public Optional<ExpenditureResolution> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<ExpenditureResolution> findByResolutionDate(LocalDate date) {
            return store.values().stream()
                    .filter(r -> date.equals(r.getResolutionDate()))
                    .sorted(Comparator.comparing(ExpenditureResolution::getId))
                    .toList();
        }

        @Override
        public List<ExpenditureResolution> findByResolutionDateBetween(LocalDate startDate, LocalDate endDate) {
            return store.values().stream()
                    .filter(r -> !r.getResolutionDate().isBefore(startDate) && !r.getResolutionDate().isAfter(endDate))
                    .sorted(Comparator.comparing(ExpenditureResolution::getId))
                    .toList();
        }
    }
}
