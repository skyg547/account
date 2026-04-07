package com.ho.account.contracts.source;

import java.util.Map;
import java.util.Optional;

public interface SourceDocumentProvider {

    boolean supports(String lineageSourceType);

    Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId);
}
