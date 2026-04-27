package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.asset.application.port.out.FixedAssetPersistencePort;
import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.BoundedContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
public class AssetSourceDocumentProvider implements SourceDocumentProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of("FIXED_ASSET", "IFRS16_LEASE");

    private final FixedAssetPersistencePort fixedAssetPersistencePort;
    private final LeasePersistencePort leasePersistencePort;

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
                case "FIXED_ASSET" -> fixedAssetPersistencePort.findById(id).map(asset -> {
                    documentDetails.put("type", "FixedAsset");
                    documentDetails.put("data", asset);
                    return documentDetails;
                });
                case "IFRS16_LEASE" -> leasePersistencePort.findContractById(id).map(contract -> {
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
