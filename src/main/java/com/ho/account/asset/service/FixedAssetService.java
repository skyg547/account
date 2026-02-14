package com.ho.account.asset.service;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import com.ho.account.basic.repository.AccountSubjectRepository; // Added for fetching account subjects
import com.ho.account.basic.domain.AccountSubject; // Added for fetching account subjects
import com.ho.account.journal.domain.JournalEntryStatus; // Added for journal entry status
import com.ho.account.basic.repository.DepartmentRepository; // Added for Department
import com.ho.account.basic.domain.Department; // Added for Department

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final JournalService journalService;
    private final AccountSubjectRepository accountSubjectRepository; // Injected
    private final DepartmentRepository departmentRepository; // Injected

    @Autowired
    public FixedAssetService(FixedAssetRepository fixedAssetRepository,
                             JournalService journalService,
                             AccountSubjectRepository accountSubjectRepository, // Added
                             DepartmentRepository departmentRepository) { // Added
        this.fixedAssetRepository = fixedAssetRepository;
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository; // Initialized
        this.departmentRepository = departmentRepository; // Initialized
    }

    // 자산 등록
    public FixedAsset registerAsset(FixedAsset asset) {
        // Initialize current book value and depreciation amount per period if not set by @PrePersist
        if (asset.getCurrentBookValue() == null) {
            asset.setCurrentBookValue(asset.getAcquisitionCost());
        }
        asset.setLastDepreciationDate(null); // Newly acquired asset has no prior depreciation

        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        // 1. 초기 취득 전표 생성
        AccountSubject cashAccount = accountSubjectRepository.findById("10100") // 현금 또는 예금 계정 코드
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));

        JournalEntry acquisitionEntry = new JournalEntry();
        acquisitionEntry.setSlipDate(asset.getAcquisitionDate());
        acquisitionEntry.setAccountingDate(asset.getAcquisitionDate());
        acquisitionEntry.setDescription("고정자산 취득: " + asset.getAssetName() + " (" + asset.getAssetCode() + ")");
        acquisitionEntry.setEntryType("FIXED_ASSET_ACQUISITION");
        acquisitionEntry.setLineageSourceType("FIXED_ASSET");
        acquisitionEntry.setLineageSourceId(savedAsset.getId().toString());
        acquisitionEntry.setCreatedBy("SYSTEM");
        acquisitionEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: 고정자산 계정 (예: 기계장치, 건물)
        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(asset.getAccountSubject());
        debitDetail.setAmount(asset.getAcquisitionCost());
        debitDetail.setDepartment(asset.getDepartment());
        debitDetail.setDetailDescription("고정자산 취득");
        acquisitionEntry.addDetail(debitDetail);

        // 대변: 현금 또는 예금
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(cashAccount);
        creditDetail.setAmount(asset.getAcquisitionCost());
        creditDetail.setDepartment(asset.getDepartment());
        creditDetail.setDetailDescription("현금/예금 감소");
        acquisitionEntry.addDetail(creditDetail);

        journalService.createJournalEntry(acquisitionEntry);

        return savedAsset;
    }

    // 월별 감가상각비 계산 및 전표 생성 (배치 작업용)
    public void processMonthlyDepreciation(LocalDate accountingDate) {
        List<FixedAsset> activeAssets = fixedAssetRepository.findByStatus("ACTIVE");

        for (FixedAsset asset : activeAssets) {
            BigDecimal monthlyDepreciation = calculateMonthlyDepreciation(asset);

            if (monthlyDepreciation.compareTo(BigDecimal.ZERO) > 0) {
                // 전표 생성
                createDepreciationJournal(asset, monthlyDepreciation, accountingDate);

                // 누계액 업데이트
                asset.setAccumulatedDepreciation(asset.getAccumulatedDepreciation().add(monthlyDepreciation));
                // 현재 장부가액 업데이트
                asset.setCurrentBookValue(asset.getCurrentBookValue().subtract(monthlyDepreciation));
                // 마지막 감가상각 처리일 업데이트
                asset.setLastDepreciationDate(accountingDate);
                
                // 상각 완료 체크 (현재 장부가액이 잔존가치와 같거나 작아지면)
                if (asset.getCurrentBookValue().compareTo(asset.getResidualValue()) <= 0) {
                    asset.setStatus("FULLY_DEPRECIATED");
                    asset.setCurrentBookValue(asset.getResidualValue()); // 잔존가치로 최종 조정
                }
                
                fixedAssetRepository.save(asset);
            }
        }
    }

    // 감가상각비 계산
    private BigDecimal calculateMonthlyDepreciation(FixedAsset asset) {
        // 잔존가액이 현재 장부가액보다 클 수 없으므로, 상각 가능한 최대 금액을 제한
        BigDecimal maxDepreciableAmount = asset.getCurrentBookValue().subtract(asset.getResidualValue());
        if (maxDepreciableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // 잔존가액에 도달했거나 그 이하인 경우 더 이상 감가상각하지 않음
        }

        BigDecimal monthlyDepreciation;

        if ("STRAIGHT_LINE".equals(asset.getDepreciationMethod())) {
            // 정액법은 asset 엔티티에 저장된 기간별 상각액을 사용
            monthlyDepreciation = asset.getDepreciationAmountPerPeriod();
        } else if ("DECLINING".equals(asset.getDepreciationMethod())) {
            // 정률법 (예: 200% 정률법 사용 시 상각률 = (1 / 내용연수) * 2)
            // 여기서는 단순화를 위해 미리 정해진 상각률을 사용하거나 동적으로 계산해야 함
            // 예시: 내용연수가 5년이면 상각률은 40% (200% 정률법)
            // 월별 상각액 = (현재 장부가액 - 잔존가치) * (연간 상각률 / 12)
            // 연간 상각률은 내용연수에 따라 달라지므로, AssetCategory 또는 시스템 설정에서 관리하는 것이 일반적
            // 여기서는 임시로 내용연수에 따른 고정 상각률을 가정 (실제로는 더 복잡)
            BigDecimal annualDepreciationRate;
            switch (asset.getUsefulLife()) {
                case 5: annualDepreciationRate = new BigDecimal("0.451"); break; // 200% 정률법 5년
                case 10: annualDepreciationRate = new BigDecimal("0.200"); break; // 200% 정률법 10년 (예시)
                default: annualDepreciationRate = new BigDecimal("0.200"); // 기본값
            }

            // (현재 장부가액 - 잔존가치)에 상각률 적용
            BigDecimal depreciableBase = asset.getCurrentBookValue().subtract(asset.getResidualValue());
            monthlyDepreciation = depreciableBase.multiply(annualDepreciationRate)
                                                .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        } else {
            monthlyDepreciation = BigDecimal.ZERO;
        }

        // 계산된 감가상각액이 상각 가능한 최대 금액을 초과하지 않도록 조정
        return monthlyDepreciation.min(maxDepreciableAmount);
    }

    // 전표 생성 로직
    private void createDepreciationJournal(FixedAsset asset, BigDecimal amount, LocalDate date) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(date);
        entry.setAccountingDate(date);
        entry.setDescription("감가상각비: " + asset.getAssetName());

        // 차변: 감가상각비 (비용)
        JournalDetail debit = new JournalDetail();
        debit.setDrcrType("DEBIT");
        debit.setAccountSubject(asset.getExpenseAccount());
        debit.setAmount(amount);
        debit.setDepartment(asset.getDepartment());
        debit.setDetailDescription("감가상각비");
        entry.addDetail(debit);

        // 대변: 감가상각누계액 (자산 차감)
        JournalDetail credit = new JournalDetail();
        credit.setDrcrType("CREDIT");
        credit.setAccountSubject(asset.getAccumulatedAccount());
        credit.setAmount(amount);
        credit.setDepartment(asset.getDepartment());
        credit.setDetailDescription("감가상각누계액");
        entry.addDetail(credit);

        JournalEntry savedEntry = journalService.createJournalEntry(entry);
        journalService.requestApproval(savedEntry.getId());
        journalService.approveJournalEntry(savedEntry.getId());
    }

    /**
     * 고정자산 처리를 수행하고 관련 전표를 생성합니다.
     *
     * @param assetId 처분할 고정자산 ID
     * @param disposalDate 처분일
     * @param salePrice 처분가액 (없는 경우 0으로 간주)
     * @return 처분된 고정자산 엔티티
     */
    public FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice) {
        FixedAsset asset = fixedAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("FixedAsset not found with ID: " + assetId));

        if (!"ACTIVE".equals(asset.getStatus()) && !"FULLY_DEPRECIATED".equals(asset.getStatus())) {
            throw new IllegalStateException("Asset is not in a state to be disposed: " + asset.getStatus());
        }

        // 잔존 장부가액
        BigDecimal bookValue = asset.getAcquisitionCost().subtract(asset.getAccumulatedDepreciation());
        // 처분 손익 계산
        BigDecimal gainOrLoss = salePrice.subtract(bookValue);

        asset.setStatus("DISPOSED");
        fixedAssetRepository.save(asset);

        // 1. 처분 전표 생성
        AccountSubject disposalGainAccount = accountSubjectRepository.findById("91100") // 자산처분이익 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Disposal Gain not found"));
        AccountSubject disposalLossAccount = accountSubjectRepository.findById("92100") // 자산처분손실 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Disposal Loss not found"));
        AccountSubject cashAccount = accountSubjectRepository.findById("10100") // 현금 또는 예금 계정 코드
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));

        JournalEntry disposalEntry = new JournalEntry();
        disposalEntry.setSlipDate(disposalDate);
        disposalEntry.setAccountingDate(disposalDate);
        disposalEntry.setDescription("고정자산 처분: " + asset.getAssetName() + " (" + asset.getAssetCode() + ")");
        disposalEntry.setEntryType("FIXED_ASSET_DISPOSAL");
        disposalEntry.setLineageSourceType("FIXED_ASSET");
        disposalEntry.setLineageSourceId(asset.getId().toString());
        disposalEntry.setCreatedBy("SYSTEM");
        disposalEntry.setStatus(JournalEntryStatus.DRAFT);

        // 대변: 취득원가 제거
        JournalDetail assetCredit = new JournalDetail();
        assetCredit.setDrcrType("CREDIT");
        assetCredit.setAccountSubject(asset.getAccountSubject());
        assetCredit.setAmount(asset.getAcquisitionCost());
        assetCredit.setDepartment(asset.getDepartment());
        assetCredit.setDetailDescription("고정자산 취득원가 제거");
        disposalEntry.addDetail(assetCredit);

        // 차변: 감가상각누계액 제거
        JournalDetail accumulatedDepreciationDebit = new JournalDetail();
        accumulatedDepreciationDebit.setDrcrType("DEBIT");
        accumulatedDepreciationDebit.setAccountSubject(asset.getAccumulatedAccount());
        accumulatedDepreciationDebit.setAmount(asset.getAccumulatedDepreciation());
        accumulatedDepreciationDebit.setDepartment(asset.getDepartment());
        accumulatedDepreciationDebit.setDetailDescription("감가상각누계액 제거");
        disposalEntry.addDetail(accumulatedDepreciationDebit);

        // 처분가액 (현금 유입)
        if (salePrice.compareTo(BigDecimal.ZERO) > 0) {
            JournalDetail cashDebit = new JournalDetail();
            cashDebit.setDrcrType("DEBIT");
            cashDebit.setAccountSubject(cashAccount);
            cashDebit.setAmount(salePrice);
            cashDebit.setDepartment(asset.getDepartment());
            cashDebit.setDetailDescription("고정자산 처분가액");
            disposalEntry.addDetail(cashDebit);
        }

        // 처분 손익
        if (gainOrLoss.compareTo(BigDecimal.ZERO) > 0) { // 이익
            JournalDetail gainCredit = new JournalDetail();
            gainCredit.setDrcrType("CREDIT");
            gainCredit.setAccountSubject(disposalGainAccount);
            gainCredit.setAmount(gainOrLoss);
            gainCredit.setDepartment(asset.getDepartment());
            gainCredit.setDetailDescription("고정자산 처분이익");
            disposalEntry.addDetail(gainCredit);
        } else if (gainOrLoss.compareTo(BigDecimal.ZERO) < 0) { // 손실
            JournalDetail lossDebit = new JournalDetail();
            lossDebit.setDrcrType("DEBIT");
            lossDebit.setAccountSubject(disposalLossAccount);
            lossDebit.setAmount(gainOrLoss.abs());
            lossDebit.setDepartment(asset.getDepartment());
            lossDebit.setDetailDescription("고정자산 처분손실");
            disposalEntry.addDetail(lossDebit);
        }

        journalService.createJournalEntry(disposalEntry);
        journalService.requestApproval(disposalEntry.getId());
        journalService.approveJournalEntry(disposalEntry.getId());

        return asset;
    }
}
