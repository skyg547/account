package com.accounting.system.asset.service;

import com.accounting.system.asset.domain.FixedAsset;
import com.accounting.system.asset.repository.FixedAssetRepository;
import com.accounting.system.journal.domain.JournalDetail;
import com.accounting.system.journal.domain.JournalEntry;
import com.accounting.system.journal.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final JournalService journalService;

    @Autowired
    public FixedAssetService(FixedAssetRepository fixedAssetRepository, JournalService journalService) {
        this.fixedAssetRepository = fixedAssetRepository;
        this.journalService = journalService;
    }

    // 자산 등록
    public FixedAsset registerAsset(FixedAsset asset) {
        return fixedAssetRepository.save(asset);
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
                
                // 상각 완료 체크
                if (asset.getAcquisitionCost().subtract(asset.getResidualValue())
                        .compareTo(asset.getAccumulatedDepreciation()) <= 0) {
                    asset.setStatus("FULLY_DEPRECIATED");
                }
                
                fixedAssetRepository.save(asset);
            }
        }
    }

    // 감가상각비 계산 (정액법 예시)
    private BigDecimal calculateMonthlyDepreciation(FixedAsset asset) {
        if ("STRAIGHT_LINE".equals(asset.getDepreciationMethod())) {
            // (취득원가 - 잔존가치) / (내용연수 * 12)
            BigDecimal depreciableAmount = asset.getAcquisitionCost().subtract(asset.getResidualValue());
            BigDecimal totalMonths = BigDecimal.valueOf(asset.getUsefulLife() * 12L);
            return depreciableAmount.divide(totalMonths, 2, RoundingMode.HALF_UP);
        }
        // 정률법 등 다른 방식은 추가 구현 필요
        return BigDecimal.ZERO;
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
}
