package com.ho.account.asset.service;

import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.AssetHistoryRepository;
import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.basic.domain.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 고정자산 서비스 (Fixed Asset Service)
 * 자산 등록, 감가상각 실행 및 이력 관리를 담당합니다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    /**
     * 자산 취득 등록
     */
    public FixedAsset registerAsset(FixedAsset asset) {
        FixedAsset savedAsset = fixedAssetRepository.save(asset);
        
        // 1. 이력 생성 (ACQUISITION)
        createHistory(savedAsset, "ACQUISITION", null, savedAsset.getDepartment(), null, savedAsset.getStatus(), "자산 신규 취득");

        // 2. Kafka 이벤트 발행
        sendKafkaEvent("FIXED_ASSET_ACQUISITION", savedAsset, savedAsset.getAcquisitionCost(), savedAsset.getAcquisitionDate());
        
        return savedAsset;
    }

    /**
     * 감가상각 실행
     */
    public void processMonthlyDepreciation(LocalDate accountingDate) {
        List<FixedAsset> activeAssets = fixedAssetRepository.findByStatus("ACTIVE");
        for (FixedAsset asset : activeAssets) {
            BigDecimal amount = asset.depreciate(accountingDate);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                sendDepreciationEvent(asset, amount, accountingDate);
                fixedAssetRepository.save(asset);
            }
        }
    }

    /**
     * 부서 이동 처리
     */
    public void changeDepartment(Long assetId, Department newDept, String reason) {
        FixedAsset asset = fixedAssetRepository.findById(assetId).orElseThrow();
        Department oldDept = asset.getDepartment();
        
        asset.setDepartment(newDept);
        fixedAssetRepository.save(asset);

        createHistory(asset, "DEPT_CHANGE", oldDept, newDept, asset.getStatus(), asset.getStatus(), reason);
        log.info("Asset {} moved from {} to {}", asset.getAssetCode(), oldDept.getName(), newDept.getName());
    }

    /**
     * 자산 처분
     */
    public FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice) {
        FixedAsset asset = fixedAssetRepository.findById(assetId).orElseThrow();
        String oldStatus = asset.getStatus();

        asset.setStatus("DISPOSED");
        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        createHistory(savedAsset, "DISPOSAL", null, null, oldStatus, "DISPOSED", "자산 처분 (매각가: " + salePrice + ")");

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "FIXED_ASSET_DISPOSAL");
        event.put("assetId", asset.getId());
        event.put("amount", asset.getAcquisitionCost());
        event.put("accumulatedAmount", asset.getAccumulatedDepreciation());
        event.put("salePrice", salePrice);
        event.put("accountingDate", disposalDate.toString());
        event.put("deptCode", asset.getDepartment().getCode());
        kafkaTemplate.send(TOPIC, event);
        
        return savedAsset;
    }

    private void createHistory(FixedAsset asset, String type, Department oldDept, Department newDept, String oldStatus, String newStatus, String desc) {
        AssetHistory history = new AssetHistory();
        history.setAsset(asset);
        history.setHistoryType(type);
        history.setOldDepartment(oldDept);
        history.setNewDepartment(newDept);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setDescription(desc);
        history.setEventAt(LocalDateTime.now());
        history.setAuditUser("SYSTEM");
        assetHistoryRepository.save(history);
    }

    private void sendKafkaEvent(String type, FixedAsset asset, BigDecimal amount, LocalDate date) {
        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", type);
        event.put("assetId", asset.getId());
        event.put("assetCode", asset.getAssetCode());
        event.put("amount", amount);
        event.put("accountingDate", date.toString());
        event.put("deptCode", asset.getDepartment().getCode());
        kafkaTemplate.send(TOPIC, event);
    }

    private void sendDepreciationEvent(FixedAsset asset, BigDecimal amount, LocalDate date) {
        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "FIXED_ASSET_DEPRECIATION");
        event.put("assetId", asset.getId());
        event.put("amount", amount);
        event.put("accountingDate", date.toString());
        event.put("deptCode", asset.getDepartment().getCode());
        event.put("expenseAccountCode", asset.getExpenseAccount().getCode());
        event.put("accumulatedAccountCode", asset.getAccumulatedAccount().getCode());
        kafkaTemplate.send(TOPIC, event);
    }
}
