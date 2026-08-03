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
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 고정자산의 등록, 상각 처리, 처분 등을 담당하는 핵심 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 회사의 '자산 관리자'입니다. 
 * 새로운 비품을 사면 장부에 등록하고(registerAsset), 
 * 매달 가치가 떨어지는 것을 계산해서 반영하며(processMonthlyDepreciation), 
 * 낡아서 팔거나 버릴 때(disposeFixedAsset)의 모든 과정을 지휘합니다. 
 * 또한 자산의 변동이 생길 때마다 회계 부서에 알리는 이벤트(Event) 발행 역할도 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetEntryService implements FixedAssetUseCase {

    private final FixedAssetPersistencePort persistencePort;
    private final AssetEventPort eventPort;

    private static final String TOPIC = "transaction-events";

    @Override
    @Transactional
    public FixedAsset registerAsset(FixedAsset asset, String actor) {
        asset.initializeAcquisitionBalances();
        asset.setStatus("ACTIVE");
        FixedAsset savedAsset = persistencePort.save(asset);

        createHistory(savedAsset, "ACQUISITION", null, asset.getDepartmentCode(), null, "ACTIVE", "Initial acquisition", actor);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "ASSET_ACQUISITION");
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("amount", savedAsset.getAcquisitionCost());
        event.put("accountingDate", savedAsset.getAcquisitionDate().toString());
        event.put("deptCode", savedAsset.getDepartmentCode());
        event.put("actor", requireActor(actor));
        
        eventPort.sendAssetEvent(TOPIC, event);
        return savedAsset;
    }

    @Override
    @Transactional
    public void processMonthlyDepreciation(LocalDate processDate, String actor) {
        persistencePort.findByStatus("ACTIVE").forEach(asset -> {
            BigDecimal amount = asset.depreciate(processDate);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                persistencePort.save(asset);
                createHistory(asset, "DEPRECIATION", null, null, null, asset.getStatus(), "Monthly depreciation", actor);
                
                Map<String, Object> event = new HashMap<>();
                event.put("transactionType", "ASSET_DEPRECIATION");
                event.put("assetCode", asset.getAssetCode());
                event.put("amount", amount);
                event.put("accountingDate", processDate.toString());
                event.put("deptCode", asset.getDepartmentCode());
                event.put("actor", requireActor(actor));
                eventPort.sendAssetEvent(TOPIC, event);
            }
        });
    }

    @Override
    @Transactional
    public FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice, String actor) {
        FixedAsset asset = persistencePort.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        
        String oldStatus = asset.getStatus();
        asset.setStatus("DISPOSED");
        FixedAsset savedAsset = persistencePort.save(asset);

        createHistory(savedAsset, "DISPOSAL", null, null, oldStatus, "DISPOSED", "Asset disposal", actor);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "ASSET_DISPOSAL");
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("salePrice", salePrice);
        event.put("bookValue", savedAsset.getCurrentBookValue());
        event.put("accountingDate", disposalDate.toString());
        event.put("deptCode", savedAsset.getDepartmentCode());
        event.put("actor", requireActor(actor));
        
        eventPort.sendAssetEvent(TOPIC, event);
        return savedAsset;
    }

    @Override
    @Transactional
    public void changeDepartment(Long assetId, String newDeptCode, String reason, String actor) {
        FixedAsset asset = persistencePort.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        
        String oldDeptCode = asset.getDepartmentCode();
        asset.setDepartmentCode(newDeptCode);
        persistencePort.save(asset);

        createHistory(asset, "TRANSFER", oldDeptCode, newDeptCode, null, null, reason, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FixedAsset> findByStatus(String status) {
        String normalizedStatus = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase();
        return persistencePort.findByStatus(normalizedStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FixedAsset> findById(Long id) {
        return persistencePort.findById(id);
    }

    private void createHistory(FixedAsset asset, String type, String oldDeptCode, String newDeptCode, String oldStatus, String newStatus, String desc, String actor) {
        AssetHistory history = new AssetHistory();
        history.setFixedAsset(asset);
        history.setHistoryType(type);
        history.setOldDepartmentCode(oldDeptCode);
        history.setNewDepartmentCode(newDeptCode);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setDescription(desc);
        history.setEventAt(LocalDateTime.now());
        history.setAuditUser(requireActor(actor));
        persistencePort.saveHistory(history);
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("asset operation actor is required");
        }
        return actor.trim();
    }
}
