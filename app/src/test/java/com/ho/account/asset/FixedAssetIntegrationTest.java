package com.ho.account.asset;

import com.ho.account.AccountApplication;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.asset.service.FixedAssetService;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = AccountApplication.class)
@Transactional
public class FixedAssetIntegrationTest {

    @Autowired
    private FixedAssetService fixedAssetService;
    @Autowired
    private FixedAssetRepository fixedAssetRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private AccountSubjectRepository accountSubjectRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private Department testDepartment;
    private AccountSubject fixedAssetAccount; // 예: 기계장치
    private AccountSubject accumulatedDepreciationAccount; // 예: 감가상각누계액
    private AccountSubject depreciationExpenseAccount; // 예: 감가상각비
    private AccountSubject cashAccount; // 예: 현금
    private AccountSubject disposalGainAccount; // 예: 자산처분이익
    private AccountSubject disposalLossAccount; // 예: 자산처분손실

    @BeforeEach
    void setUp() {
        // 각 테스트가 독립적으로 실행되도록 기존 전표를 정리
        journalEntryRepository.deleteAll();
        fixedAssetRepository.deleteAll(); // 고정자산 데이터도 함께 정리

        // 테스트 데이터 설정
        testDepartment = new Department();
        testDepartment.setCode("FA_DEPT");
        testDepartment.setName("고정자산 부서");
        testDepartment.setUseYn(true);
        departmentRepository.save(testDepartment);

        fixedAssetAccount = new AccountSubject();
        fixedAssetAccount.setCode("20600"); // 기계장치 (예시)
        fixedAssetAccount.setName("기계장치");
        fixedAssetAccount.setUseYn(true);
        fixedAssetAccount.setUnsettled(false);
        accountSubjectRepository.save(fixedAssetAccount);

        accumulatedDepreciationAccount = new AccountSubject();
        accumulatedDepreciationAccount.setCode("20699"); // 기계장치 감가상각누계액 (예시)
        accumulatedDepreciationAccount.setName("기계장치감가상각누계액");
        accumulatedDepreciationAccount.setUseYn(true);
        accumulatedDepreciationAccount.setUnsettled(false);
        accountSubjectRepository.save(accumulatedDepreciationAccount);

        depreciationExpenseAccount = new AccountSubject();
        depreciationExpenseAccount.setCode("53100"); // 감가상각비 (예시)
        depreciationExpenseAccount.setName("감가상각비");
        depreciationExpenseAccount.setUseYn(true);
        depreciationExpenseAccount.setUnsettled(false);
        accountSubjectRepository.save(depreciationExpenseAccount);

        cashAccount = new AccountSubject();
        cashAccount.setCode("10100"); // 현금 (예시)
        cashAccount.setName("현금및현금성자산");
        cashAccount.setUseYn(true);
        cashAccount.setUnsettled(false);
        accountSubjectRepository.save(cashAccount);

        disposalGainAccount = new AccountSubject();
        disposalGainAccount.setCode("91100"); // 자산처분이익 (예시)
        disposalGainAccount.setName("자산처분이익");
        disposalGainAccount.setUseYn(true);
        disposalGainAccount.setUnsettled(false);
        accountSubjectRepository.save(disposalGainAccount);

        disposalLossAccount = new AccountSubject();
        disposalLossAccount.setCode("92100"); // 자산처분손실 (예시)
        disposalLossAccount.setName("자산처분손실");
        disposalLossAccount.setUseYn(true);
        disposalLossAccount.setUnsettled(false);
        accountSubjectRepository.save(disposalLossAccount);
    }

    @Test
    @DisplayName("고정자산 취득 및 전표 검증")
    void testAcquireFixedAssetAndJournalEntry() {
        // 사전 조건
        FixedAsset newAsset = createFixedAsset(
                "FA001", "생산 기계", LocalDate.of(2023, 1, 1),
                new BigDecimal("12000000"), 5, "STRAIGHT_LINE", new BigDecimal("0"), testDepartment
        );

        // 실행
        FixedAsset registeredAsset = fixedAssetService.registerAsset(newAsset);

        // 검증
        assertThat(registeredAsset).isNotNull();
        assertThat(registeredAsset.getId()).isNotNull();
        assertThat(registeredAsset.getStatus()).isEqualTo("ACTIVE");
        assertThat(registeredAsset.getCurrentBookValue()).isEqualByComparingTo(newAsset.getAcquisitionCost());

        // 취득 전표 검증
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "FIXED_ASSET", registeredAsset.getId().toString());
        assertThat(entries).hasSize(1);
        JournalEntry acquisitionEntry = entries.get(0);
        assertThat(acquisitionEntry.getEntryType()).isEqualTo("FIXED_ASSET_ACQUISITION");
        assertThat(acquisitionEntry.getDetails()).hasSize(2);

        // 차변: FixedAssetAccount
        JournalDetail debitDetail = acquisitionEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT")).findFirst().get();
        assertThat(debitDetail.getAccountSubject().getCode()).isEqualTo(fixedAssetAccount.getCode());
        assertThat(debitDetail.getAmount()).isEqualByComparingTo(newAsset.getAcquisitionCost());

        // 대변: CashAccount
        JournalDetail creditDetail = acquisitionEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT")).findFirst().get();
        assertThat(creditDetail.getAccountSubject().getCode()).isEqualTo(cashAccount.getCode());
        assertThat(creditDetail.getAmount()).isEqualByComparingTo(newAsset.getAcquisitionCost());
    }

    @Test
    @DisplayName("월별 감가상각 처리 및 전표 검증 - 정액법")
    void testMonthlyDepreciationStraightLineAndJournalEntry() {
        // 사전 조건 - 1200만원, 내용연수 5년, 잔존가치 0, 정액법 -> 월 20만원 상각
        FixedAsset asset = createAndRegisterFixedAsset(
                "FA002", "배송 트럭", LocalDate.of(2023, 1, 1),
                new BigDecimal("12000000"), 5, "STRAIGHT_LINE", new BigDecimal("0"), testDepartment
        );
        Long assetId = asset.getId();

        // 실행 - 3개월 감가상각 처리
        LocalDate processDate1 = LocalDate.of(2023, 1, 31);
        fixedAssetService.processMonthlyDepreciation(processDate1);
        LocalDate processDate2 = LocalDate.of(2023, 2, 28);
        fixedAssetService.processMonthlyDepreciation(processDate2);
        LocalDate processDate3 = LocalDate.of(2023, 3, 31);
        fixedAssetService.processMonthlyDepreciation(processDate3);

        // 검증
        FixedAsset updatedAsset = fixedAssetRepository.findById(assetId).orElseThrow();
        // 12,000,000 / (5 * 12) = 200,000
        BigDecimal expectedMonthlyDepreciation = new BigDecimal("200000.00");
        BigDecimal expectedAccumulatedDepreciation = expectedMonthlyDepreciation.multiply(new BigDecimal("3")); // 3개월치
        BigDecimal expectedCurrentBookValue = asset.getAcquisitionCost().subtract(expectedAccumulatedDepreciation);

        assertThat(updatedAsset.getAccumulatedDepreciation()).isEqualByComparingTo(expectedAccumulatedDepreciation);
        assertThat(updatedAsset.getCurrentBookValue()).isEqualByComparingTo(expectedCurrentBookValue);
        assertThat(updatedAsset.getLastDepreciationDate()).isEqualTo(processDate3);
        assertThat(updatedAsset.getStatus()).isEqualTo("ACTIVE");

        // 감가상각 전표 검증
        List<JournalEntry> depreciationEntries = journalEntryRepository.findAll().stream()
                .filter(e -> "FIXED_ASSET_DEPRECIATION".equals(e.getEntryType()) && e.getLineageSourceId().equals(assetId.toString()))
                .collect(Collectors.toList());

        assertThat(depreciationEntries).hasSize(3); // 3개월치 전표
        for (JournalEntry entry : depreciationEntries) {
            assertThat(entry.getDetails()).hasSize(2);
            // 차변: 감가상각비
            JournalDetail debit = entry.getDetails().stream().filter(d -> d.getDrcrType().equals("DEBIT")).findFirst().get();
            assertThat(debit.getAccountSubject().getCode()).isEqualTo(depreciationExpenseAccount.getCode());
            assertThat(debit.getAmount()).isEqualByComparingTo(expectedMonthlyDepreciation);

            // 대변: 감가상각누계액
            JournalDetail credit = entry.getDetails().stream().filter(d -> d.getDrcrType().equals("CREDIT")).findFirst().get();
            assertThat(credit.getAccountSubject().getCode()).isEqualTo(accumulatedDepreciationAccount.getCode());
            assertThat(credit.getAmount()).isEqualByComparingTo(expectedMonthlyDepreciation);
        }
    }

    @Test
    @DisplayName("고정자산 처분 및 전표 검증 - 처분이익")
    void testDisposeFixedAssetWithGainAndJournalEntry() {
        // 사전 조건 - 3개월 감가상각된 자산
        FixedAsset asset = createAndRegisterFixedAsset(
                "FA003", "사무용 가구", LocalDate.of(2023, 1, 1),
                new BigDecimal("6000000"), 5, "STRAIGHT_LINE", new BigDecimal("0"), testDepartment
        );
        Long assetId = asset.getId();
        fixedAssetService.processMonthlyDepreciation(LocalDate.of(2023, 1, 31)); // 월 10만원 상각
        fixedAssetService.processMonthlyDepreciation(LocalDate.of(2023, 2, 28));
        fixedAssetService.processMonthlyDepreciation(LocalDate.of(2023, 3, 31));

        FixedAsset assetBeforeDisposal = fixedAssetRepository.findById(assetId).orElseThrow();
        // 취득원가 6,000,000, 누계액 300,000 (10만원 * 3개월) -> 장부가액 5,700,000
        BigDecimal acquisitionCost = assetBeforeDisposal.getAcquisitionCost();
        BigDecimal accumulatedDepreciation = assetBeforeDisposal.getAccumulatedDepreciation();
        BigDecimal bookValue = acquisitionCost.subtract(accumulatedDepreciation); // 5,700,000

        BigDecimal salePrice = new BigDecimal("6000000"); // 처분가액 6,000,000 (처분이익 300,000)
        LocalDate disposalDate = LocalDate.of(2023, 4, 1);

        // 실행
        FixedAsset disposedAsset = fixedAssetService.disposeFixedAsset(assetId, disposalDate, salePrice);

        // 검증
        assertThat(disposedAsset.getStatus()).isEqualTo("DISPOSED");
        assertThat(fixedAssetRepository.findById(assetId).orElseThrow().getStatus()).isEqualTo("DISPOSED");

        // 처분 전표 검증
        List<JournalEntry> disposalEntries = journalEntryRepository.findAll().stream()
                .filter(e -> "FIXED_ASSET_DISPOSAL".equals(e.getEntryType()) && e.getLineageSourceId().equals(assetId.toString()))
                .collect(Collectors.toList());
        assertThat(disposalEntries).hasSize(1);
        JournalEntry disposalEntry = disposalEntries.get(0);
        assertThat(disposalEntry.getAccountingDate()).isEqualTo(disposalDate);

        // 상세 검증
        // 대변: FixedAssetAccount(취득원가)
        JournalDetail assetCredit = disposalEntry.getDetails().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(fixedAssetAccount.getCode()) && d.getDrcrType().equals("CREDIT")).findFirst().get();
        assertThat(assetCredit.getAmount()).isEqualByComparingTo(acquisitionCost);

        // 차변: AccumulatedDepreciationAccount
        JournalDetail accumulatedDepreciationDebit = disposalEntry.getDetails().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(accumulatedDepreciationAccount.getCode()) && d.getDrcrType().equals("DEBIT")).findFirst().get();
        assertThat(accumulatedDepreciationDebit.getAmount()).isEqualByComparingTo(accumulatedDepreciation);

        // 차변: CashAccount(매각가액)
        JournalDetail cashDebit = disposalEntry.getDetails().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(cashAccount.getCode()) && d.getDrcrType().equals("DEBIT")).findFirst().get();
        assertThat(cashDebit.getAmount()).isEqualByComparingTo(salePrice);

        // 대변: DisposalGainAccount(처분이익)
        JournalDetail gainCredit = disposalEntry.getDetails().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(disposalGainAccount.getCode()) && d.getDrcrType().equals("CREDIT")).findFirst().get();
        assertThat(gainCredit.getAmount()).isEqualByComparingTo(salePrice.subtract(bookValue)); // 300,000
    }

    @Test
    @DisplayName("고정자산 처분 및 전표 검증 - 처분손실")
    void testDisposeFixedAssetWithLossAndJournalEntry() {
        // 사전 조건 - 3개월 감가상각된 자산
        FixedAsset asset = createAndRegisterFixedAsset(
                "FA004", "컴퓨터", LocalDate.of(2023, 1, 1),
                new BigDecimal("3000000"), 5, "STRAIGHT_LINE", new BigDecimal("0"), testDepartment
        );
        Long assetId = asset.getId();
        fixedAssetService.processMonthlyDepreciation(LocalDate.of(2023, 1, 31)); // 월 5만원 상각
        fixedAssetService.processMonthlyDepreciation(LocalDate.of(2023, 2, 28));
        fixedAssetService.processMonthlyDepreciation(LocalDate.of(2023, 3, 31));

        FixedAsset assetBeforeDisposal = fixedAssetRepository.findById(assetId).orElseThrow();
        // 취득원가 3,000,000, 누계액 150,000 (5만원 * 3개월) -> 장부가액 2,850,000
        BigDecimal acquisitionCost = assetBeforeDisposal.getAcquisitionCost();
        BigDecimal accumulatedDepreciation = assetBeforeDisposal.getAccumulatedDepreciation();
        BigDecimal bookValue = acquisitionCost.subtract(accumulatedDepreciation); // 2,850,000

        BigDecimal salePrice = new BigDecimal("2500000"); // 처분가액 2,500,000 (처분손실 350,000)
        LocalDate disposalDate = LocalDate.of(2023, 4, 1);

        // 실행
        FixedAsset disposedAsset = fixedAssetService.disposeFixedAsset(assetId, disposalDate, salePrice);

        // 검증
        assertThat(disposedAsset.getStatus()).isEqualTo("DISPOSED");

        // 처분 전표 검증
        List<JournalEntry> disposalEntries = journalEntryRepository.findAll().stream()
                .filter(e -> "FIXED_ASSET_DISPOSAL".equals(e.getEntryType()) && e.getLineageSourceId().equals(assetId.toString()))
                .collect(Collectors.toList());
        assertThat(disposalEntries).hasSize(1);
        JournalEntry disposalEntry = disposalEntries.get(0);

        // 차변: DisposalLossAccount
        JournalDetail lossDebit = disposalEntry.getDetails().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(disposalLossAccount.getCode()) && d.getDrcrType().equals("DEBIT")).findFirst().get();
        assertThat(lossDebit.getAmount()).isEqualByComparingTo(bookValue.subtract(salePrice)); // 350,000
    }


    private FixedAsset createFixedAsset(String assetCode, String assetName, LocalDate acquisitionDate,
                                        BigDecimal acquisitionCost, Integer usefulLife, String depreciationMethod,
                                        BigDecimal residualValue, Department department) {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode(assetCode);
        asset.setAssetName(assetName);
        asset.setAccountSubject(fixedAssetAccount);
        asset.setAccumulatedAccount(accumulatedDepreciationAccount);
        asset.setExpenseAccount(depreciationExpenseAccount);
        asset.setAcquisitionDate(acquisitionDate);
        asset.setAcquisitionCost(acquisitionCost);
        asset.setUsefulLife(usefulLife);
        asset.setDepreciationMethod(depreciationMethod);
        asset.setResidualValue(residualValue);
        asset.setDepartment(department);
        return asset;
    }

    private FixedAsset createAndRegisterFixedAsset(String assetCode, String assetName, LocalDate acquisitionDate,
                                                  BigDecimal acquisitionCost, Integer usefulLife, String depreciationMethod,
                                                  BigDecimal residualValue, Department department) {
        FixedAsset asset = createFixedAsset(assetCode, assetName, acquisitionDate, acquisitionCost, usefulLife,
                depreciationMethod, residualValue, department);
        return fixedAssetService.registerAsset(asset);
    }
}
