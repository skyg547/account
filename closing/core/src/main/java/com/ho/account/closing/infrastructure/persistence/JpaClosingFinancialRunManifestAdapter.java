package com.ho.account.closing.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.closing.application.port.out.ClosingFinancialRunManifestPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaClosingFinancialRunManifestAdapter implements ClosingFinancialRunManifestPort {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final TypeReference<List<ClosingJournalEntryCommand>> COMMANDS = new TypeReference<>() { };
    private final ClosingFinancialRunManifestRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean exists(String scope, Long batchId) {
        return repository.existsByScopeAndBatchId(scope, batchId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClosingJournalEntryCommand> loadOrCreate(String scope, Long batchId,
                                                          Supplier<List<ClosingJournalEntryCommand>> prepare) {
        return repository.findByScopeAndBatchId(scope, batchId)
                .map(row -> deserialize(row.getCommandsJson()))
                .orElseGet(() -> {
                    List<ClosingJournalEntryCommand> commands = List.copyOf(prepare.get());
                    repository.saveAndFlush(new ClosingFinancialRunManifest(scope, batchId, serialize(commands)));
                    return commands;
                });
    }

    private String serialize(List<ClosingJournalEntryCommand> commands) {
        try {
            return JSON.writeValueAsString(commands);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot persist closing command manifest", e);
        }
    }

    private List<ClosingJournalEntryCommand> deserialize(String json) {
        try {
            return List.copyOf(JSON.readValue(json, COMMANDS));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read closing command manifest", e);
        }
    }
}
