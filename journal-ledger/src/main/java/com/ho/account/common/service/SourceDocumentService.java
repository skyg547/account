package com.ho.account.common.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SourceDocumentService {

    private final List<SourceDocumentProvider> sourceDocumentProviders;

    public SourceDocumentService(List<SourceDocumentProvider> sourceDocumentProviders) {
        this.sourceDocumentProviders = sourceDocumentProviders;
    }

    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        return sourceDocumentProviders.stream()
                .filter(provider -> provider.supports(lineageSourceType))
                .findFirst()
                .flatMap(provider -> provider.getSourceDocument(lineageSourceType, lineageSourceId));
    }
}
