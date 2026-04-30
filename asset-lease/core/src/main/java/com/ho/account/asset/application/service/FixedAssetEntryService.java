package com.ho.account.asset.application.service;

import com.ho.account.asset.application.port.in.FixedAssetUseCase;
import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.FixedAssetPersistencePort;
import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class FixedAssetEntryService implements FixedAssetUseCase {

    private final FixedAssetPersistencePort persistencePort;
    private final AssetEventPort eventPort;

    private static final String TOPIC = "transaction-events";

    @Override
    @Transactional
    public FixedAsset registerAsset(FixedAsset asset) {
        asset.setStatus("ACTIVE");
        FixedAsset savedAsset = persistencePort.save(asset);

        createHistory(savedAsset, "ACQUISITION", null, asset.getDepartmentCode(), null, "ACTIVE", "Initial acquisition");

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "ASSET_ACQUISITION");
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("amount", savedAsset.getAcquisitionCost());
        event.put("accountingDate", savedAsset.getAcquisitionDate().toString());
        event.put("deptCode", savedAsset.getDepartmentCode());
        
        eventPort.sendAssetEvent(TOPIC, event);
        return savedAsset;
    }

    @Override
    @Transactional
    public void processMonthlyDepreciation(LocalDate processDate) {
        persistencePort.findByStatus("ACTIVE").forEach(asset -> {
            BigDecimal amount = asset.depreciate(processDate);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                persistencePort.save(asset);
                createHistory(asset, "DEPRECIATION", null, null, null, asset.getStatus(), "Monthly depreciation");
                
                Map<String, Object> event = new HashMap<>();
                event.put("transactionType", "ASSET_DEPRECIATION");
                event.put("assetCode", asset.getAssetCode());
                event.put("amount", amount);
                event.put("accountingDate", processDate.toString());
                event.put("deptCode", asset.getDepartmentCode());
                eventPort.sendAssetEvent(TOPIC, event);
            }
        });
    }

    @Override
    @Transactional
    public FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice) {
        FixedAsset asset = persistencePort.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        
        String oldStatus = asset.getStatus();
        asset.setStatus("DISPOSED");
        FixedAsset savedAsset = persistencePort.save(asset);

        createHistory(savedAsset, "DISPOSAL", null, null, oldStatus, "DISPOSED", "Asset disposal");

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "ASSET_DISPOSAL");
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("salePrice", salePrice);
        event.put("bookValue", savedAsset.getCurrentBookValue());
        event.put("accountingDate", disposalDate.toString());
        event.put("deptCode", savedAsset.getDepartmentCode());
        
        eventPort.sendAssetEvent(TOPIC, event);
        return savedAsset;
    }

    @Override
    @Transactional
    public void changeDepartment(Long assetId, String newDeptCode, String reason) {
        FixedAsset asset = persistencePort.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        
        String oldDeptCode = asset.getDepartmentCode();
        asset.setDepartmentCode(newDeptCode);
        persistencePort.save(asset);

        createHistory(asset, "TRANSFER", oldDeptCode, newDeptCode, null, null, reason);
    }

    private void createHistory(FixedAsset asset, String type, String oldDeptCode, String newDeptCode, String oldStatus, String newStatus, String desc) {
        AssetHistory history = new AssetHistory();
        history.setFixedAsset(asset);
        history.setHistoryType(type);
        history.setOldDepartmentCode(oldDeptCode);
        history.setNewDepartmentCode(newDeptCode);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setDescription(desc);
        history.setEventAt(LocalDateTime.now());
        history.setAuditUser("SYSTEM");
        persistencePort.saveHistory(history);
    }
}
