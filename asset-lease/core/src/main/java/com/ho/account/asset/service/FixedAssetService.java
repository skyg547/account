package com.ho.account.asset.service;

import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.AssetHistoryRepository;
import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.masterdata.core.domain.model.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    @Transactional
    public FixedAsset registerAsset(FixedAsset asset) {
        asset.setStatus("ACTIVE");
        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        createHistory(savedAsset, "ACQUISITION", null, asset.getDepartment(), null, "ACTIVE", "Initial acquisition");

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "ASSET_ACQUISITION");
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("amount", savedAsset.getAcquisitionCost());
        event.put("accountingDate", savedAsset.getAcquisitionDate().toString());
        event.put("deptCode", savedAsset.getDepartment().getCode());
        
        kafkaTemplate.send(TOPIC, event);
        return savedAsset;
    }

    @Transactional
    public void processMonthlyDepreciation(LocalDate processDate) {
        fixedAssetRepository.findByStatus("ACTIVE").forEach(asset -> {
            BigDecimal amount = asset.depreciate(processDate);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                fixedAssetRepository.save(asset);
                createHistory(asset, "DEPRECIATION", null, null, null, asset.getStatus(), "Monthly depreciation");
                
                Map<String, Object> event = new HashMap<>();
                event.put("transactionType", "ASSET_DEPRECIATION");
                event.put("assetCode", asset.getAssetCode());
                event.put("amount", amount);
                event.put("accountingDate", processDate.toString());
                event.put("deptCode", asset.getDepartment().getCode());
                kafkaTemplate.send(TOPIC, event);
            }
        });
    }

    @Transactional
    public FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice) {
        FixedAsset asset = fixedAssetRepository.findById(assetId).orElseThrow();
        String oldStatus = asset.getStatus();
        asset.setStatus("DISPOSED");
        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        createHistory(savedAsset, "DISPOSAL", null, null, oldStatus, "DISPOSED", "Asset disposal");

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "ASSET_DISPOSAL");
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("salePrice", salePrice);
        event.put("bookValue", savedAsset.getCurrentBookValue());
        event.put("accountingDate", disposalDate.toString());
        event.put("deptCode", savedAsset.getDepartment().getCode());
        
        kafkaTemplate.send(TOPIC, event);
        return savedAsset;
    }

    @Transactional
    public void changeDepartment(Long assetId, Department newDept, String reason) {
        FixedAsset asset = fixedAssetRepository.findById(assetId).orElseThrow();
        Department oldDept = asset.getDepartment();
        asset.setDepartment(newDept);
        fixedAssetRepository.save(asset);

        createHistory(asset, "TRANSFER", oldDept, newDept, null, null, reason);
    }

    private void createHistory(FixedAsset asset, String type, Department oldDept, Department newDept, String oldStatus, String newStatus, String desc) {
        AssetHistory history = new AssetHistory();
        history.setFixedAsset(asset);
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
}
