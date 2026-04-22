package com.ho.account.asset.service;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 고정자산 서비스 (Fixed Asset Service)
 * 자산 등록, 감가상각 실행 및 Kafka 이벤트를 통한 전표 자동 연계 담당.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    /**
     * 자산 취득 등록 및 취득 전표 이벤트 발행
     */
    public FixedAsset registerAsset(FixedAsset asset) {
        FixedAsset savedAsset = fixedAssetRepository.save(asset);
        log.info("Asset registered: {} ({})", asset.getAssetName(), asset.getAssetCode());

        // Kafka 이벤트 발행: 취득 전표 생성용
        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "FIXED_ASSET_ACQUISITION");
        event.put("assetId", savedAsset.getId());
        event.put("assetCode", savedAsset.getAssetCode());
        event.put("assetName", savedAsset.getAssetName());
        event.put("amount", savedAsset.getAcquisitionCost());
        event.put("accountingDate", savedAsset.getAcquisitionDate().toString());
        event.put("deptCode", savedAsset.getDepartment().getCode());
        event.put("assetAccountCode", savedAsset.getAccountSubject().getCode());
        
        kafkaTemplate.send(TOPIC, event);
        return savedAsset;
    }

    /**
     * 특정 일자의 감가상각을 일괄 처리하고 전표 이벤트를 발행합니다.
     * (배치 작업에서 주로 호출됨)
     */
    public void processMonthlyDepreciation(LocalDate accountingDate) {
        List<FixedAsset> activeAssets = fixedAssetRepository.findByStatus("ACTIVE");
        log.info("Processing depreciation for {} assets on {}", activeAssets.size(), accountingDate);

        for (FixedAsset asset : activeAssets) {
            BigDecimal amount = asset.depreciate(accountingDate);

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                // Kafka 이벤트 발행: 감가상각비 전표 생성용
                Map<String, Object> event = new HashMap<>();
                event.put("transactionType", "FIXED_ASSET_DEPRECIATION");
                event.put("assetId", asset.getId());
                event.put("assetCode", asset.getAssetCode());
                event.put("amount", amount);
                event.put("accountingDate", accountingDate.toString());
                event.put("deptCode", asset.getDepartment().getCode());
                event.put("expenseAccountCode", asset.getExpenseAccount().getCode());
                event.put("accumulatedAccountCode", asset.getAccumulatedAccount().getCode());
                
                kafkaTemplate.send(TOPIC, event);
                fixedAssetRepository.save(asset);
            }
        }
    }

    /**
     * 자산 처분 처리 및 처분 전표 이벤트 발행
     */
    public FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice) {
        FixedAsset asset = fixedAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));

        asset.setStatus("DISPOSED");
        fixedAssetRepository.save(asset);

        // Kafka 이벤트 발행: 처분 전표 생성용
        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "FIXED_ASSET_DISPOSAL");
        event.put("assetId", asset.getId());
        event.put("amount", asset.getAcquisitionCost());
        event.put("accumulatedAmount", asset.getAccumulatedDepreciation());
        event.put("salePrice", salePrice);
        event.put("accountingDate", disposalDate.toString());
        event.put("deptCode", asset.getDepartment().getCode());
        
        kafkaTemplate.send(TOPIC, event);
        return asset;
    }
}
