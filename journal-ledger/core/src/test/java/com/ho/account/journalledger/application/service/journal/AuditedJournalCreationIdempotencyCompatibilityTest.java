package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Transport-stable RED/GREEN proof that compiles unchanged against audited source a97d10ab.
 *
 * <p>The audited source has no creation-identity type or overload, so reflection deliberately falls
 * back to its public one-argument create transport. That version saves both lost-response retries and
 * this assertion fails with two IDs/two writes. Current code discovers the identity overload and
 * converges through the durable operation contract.</p>
 */
class AuditedJournalCreationIdempotencyCompatibilityTest {

    @Test
    void repeatedEquivalentPublicCreationReturnsSameJournalAndPersistsItOnce() throws Exception {
        AtomicInteger journalWrites = new AtomicInteger();
        AtomicInteger nextId = new AtomicInteger(7650);
        Map<Long, JournalEntry> journalsById = new HashMap<>();
        JournalPersistencePort journals = Mockito.mock(JournalPersistencePort.class, invocation -> {
            return switch (invocation.getMethod().getName()) {
                case "save" -> {
                    JournalEntry journal = invocation.getArgument(0);
                    journal.setId((long) nextId.incrementAndGet());
                    journal.setSlipNo("JE-AUDITED-" + journal.getId());
                    journal.initializeDraft();
                    journalsById.put(journal.getId(), journal);
                    journalWrites.incrementAndGet();
                    yield journal;
                }
                case "findById", "findByIdWithDetailsWithoutLock" ->
                        Optional.ofNullable(journalsById.get((Long) invocation.getArgument(0)));
                case "nextSlipNumber" -> 1L;
                default -> Mockito.RETURNS_DEFAULTS.answer(invocation);
            };
        });
        AtomicReference<Object> creationOperation = new AtomicReference<>();
        JournalEntryService service = newService(journals, creationOperation);

        JournalEntry first = invokePublicCreate(service, request());
        JournalEntry retry = invokePublicCreate(service, request());

        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(retry.getDetails()).hasSize(2);
        assertThat(journalWrites).hasValue(1);
    }

    private static JournalEntryService newService(
            JournalPersistencePort journals,
            AtomicReference<Object> creationOperation) throws Exception {
        Constructor<?> constructor = Arrays.stream(JournalEntryService.class.getConstructors())
                .max(java.util.Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        Object[] dependencies = Arrays.stream(constructor.getParameterTypes())
                .map(type -> dependency(type, journals, creationOperation))
                .toArray();
        return (JournalEntryService) constructor.newInstance(dependencies);
    }

    private static Object dependency(
            Class<?> type,
            JournalPersistencePort journals,
            AtomicReference<Object> creationOperation) {
        if (type == JournalPersistencePort.class) return journals;
        if (type == JournalRuleEngine.class) return Mockito.mock(JournalRuleEngine.class);
        if (type == PostingService.class) return Mockito.mock(PostingService.class);
        if (type == JournalValidationEngine.class) return Mockito.mock(JournalValidationEngine.class);
        if (type.getSimpleName().equals("JournalCreationOperationPersistencePort")) {
            return Mockito.mock(type, invocation -> switch (invocation.getMethod().getName()) {
                case "acquireOperationLock" -> null;
                case "findByIdentity" -> Optional.ofNullable(creationOperation.get());
                case "save" -> {
                    Object operation = invocation.getArgument(0);
                    creationOperation.set(operation);
                    yield operation;
                }
                default -> Mockito.RETURNS_DEFAULTS.answer(invocation);
            });
        }
        return Mockito.mock(type);
    }

    private static JournalEntry invokePublicCreate(JournalEntryService service, JournalEntry request)
            throws Exception {
        try {
            Class<?> identityType = Class.forName(
                    "com.ho.account.journalledger.application.port.in.JournalCreationIdentity");
            Object identity = identityType.getConstructor(String.class, String.class, String.class)
                    .newInstance("ISSUE_765", "AUDITED-COMPAT", "PRIMARY");
            Method currentCreate = JournalEntryService.class.getMethod(
                    "createJournalEntry", identityType, JournalEntry.class);
            return invoke(service, currentCreate, identity, request);
        } catch (ClassNotFoundException | NoSuchMethodException auditedSource) {
            Method auditedCreate = JournalEntryService.class.getMethod(
                    "createJournalEntry", JournalEntry.class);
            return invoke(service, auditedCreate, request);
        }
    }

    private static JournalEntry invoke(JournalEntryService service, Method method, Object... arguments)
            throws Exception {
        try {
            return (JournalEntry) method.invoke(service, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) throw checked;
            throw exception;
        }
    }

    private static JournalEntry request() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setAccountingDate(LocalDate.of(2026, 9, 25));
        entry.setDescription("audited creation retry");
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("KRW");
        entry.setExchangeRate(BigDecimal.ONE);
        entry.setCreatedBy("service:audited-maker");
        entry.setAuditUser("service:audited-maker");
        entry.setLineageSourceType("ISSUE_765");
        entry.setLineageSourceId("AUDITED-COMPAT");
        entry.addDetail(detail(JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100"));
        return entry;
    }

    private static JournalDetail detail(JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        detail.setAuditUser("service:audited-maker");
        return detail;
    }
}
