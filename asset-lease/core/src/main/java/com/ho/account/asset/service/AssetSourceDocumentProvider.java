package com.ho.account.asset.service;

import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.BoundedContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class AssetSourceDocumentProvider implements SourceDocumentProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of("FIXED_ASSET", "IFRS16_LEASE");

    private final FixedAssetRepository fixedAssetRepository;
    private final LeaseContractRepository leaseContractRepository;

    public AssetSourceDocumentProvider(FixedAssetRepository fixedAssetRepository,
                                       LeaseContractRepository leaseContractRepository) {
        this.fixedAssetRepository = fixedAssetRepository;
        this.leaseContractRepository = leaseContractRepository;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        try {
            Long id = Long.valueOf(lineageSourceId);
            Map<String, Object> documentDetails = new HashMap<>();

            return switch (lineageSourceType) {
                case "FIXED_ASSET" -> fixedAssetRepository.findById(id).map(asset -> {
                    documentDetails.put("type", "FixedAsset");
                    documentDetails.put("data", asset);
                    return documentDetails;
                });
                case "IFRS16_LEASE" -> leaseContractRepository.findById(id).map(contract -> {
                    documentDetails.put("type", "LeaseContract");
                    documentDetails.put("data", contract);
                    return documentDetails;
                });
                default -> Optional.empty();
            };
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public String serviceName() {
        return "asset-lease-source-document-provider";
    }

    @Override
    public BoundedContext boundedContext() {
        return BoundedContext.ASSET_LEASE;
    }

    @Override
    public String description() {
        return "Provides fixed asset and lease lineage documents.";
    }
}
