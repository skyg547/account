package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.lang.reflect.Constructor;
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
 * Compatibility regression that compiles unchanged against audited source a97d10ab and current code.
 * New reversal operation types are deliberately accessed only through constructor reflection.
 */
class AuditedReversalIdempotencyCompatibilityTest {

    @Test
    void repeatedPublicReversalRequestReturnsSameJournalAndPersistsItOnce() throws Exception {
        JournalEntry original = postedOriginal();
        AtomicInteger journalSaveCalls = new AtomicInteger();
        AtomicInteger nextJournalId = new AtomicInteger(2);
        Map<Long, JournalEntry> savedJournals = new HashMap<>();
        JournalPersistencePort journals = Mockito.mock(JournalPersistencePort.class, invocation -> {
            return switch (invocation.getMethod().getName()) {
                case "findByIdWithDetails" -> Optional.of(original);
                case "findByIdWithDetailsWithoutLock" ->
                        Optional.ofNullable(savedJournals.get((Long) invocation.getArgument(0)));
                case "save" -> {
                    JournalEntry journal = invocation.getArgument(0);
                    journal.setId((long) nextJournalId.getAndIncrement());
                    savedJournals.put(journal.getId(), journal);
                    journalSaveCalls.incrementAndGet();
                    yield journal;
                }
                default -> Mockito.RETURNS_DEFAULTS.answer(invocation);
            };
        });
        AtomicReference<Object> reversalOperation = new AtomicReference<>();
        JournalEntryService service = newService(journals, reversalOperation);

        JournalEntry first = service.reverseJournalEntry(
                1L, LocalDate.of(2026, 9, 30), "maker", "correction");
        JournalEntry duplicate = service.reverseJournalEntry(
                1L, LocalDate.of(2026, 10, 1), "other", "retry payload");

        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThat(journalSaveCalls).hasValue(1);
    }

    private static JournalEntryService newService(
            JournalPersistencePort journals,
            AtomicReference<Object> reversalOperation) throws Exception {
        Constructor<?> constructor = Arrays.stream(JournalEntryService.class.getConstructors())
                .max(java.util.Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        Object[] dependencies = Arrays.stream(constructor.getParameterTypes())
                .map(type -> dependency(type, journals, reversalOperation))
                .toArray();
        return (JournalEntryService) constructor.newInstance(dependencies);
    }

    private static Object dependency(
            Class<?> type,
            JournalPersistencePort journals,
            AtomicReference<Object> reversalOperation) {
        if (type == JournalPersistencePort.class) return journals;
        if (type == JournalRuleEngine.class) return Mockito.mock(JournalRuleEngine.class);
        if (type == PostingService.class) return Mockito.mock(PostingService.class);
        if (type == JournalValidationEngine.class) return Mockito.mock(JournalValidationEngine.class);
        if (type.getSimpleName().equals("JournalReversalPersistencePort")) {
            return Mockito.mock(type, invocation -> switch (invocation.getMethod().getName()) {
                case "findByOriginalJournalEntryId", "findByReversalJournalEntryId" ->
                        Optional.ofNullable(reversalOperation.get());
                case "save" -> {
                    Object operation = invocation.getArgument(0);
                    reversalOperation.set(operation);
                    yield operation;
                }
                default -> Mockito.RETURNS_DEFAULTS.answer(invocation);
            });
        }
        return Mockito.mock(type);
    }

    private static JournalEntry postedOriginal() throws Exception {
        JournalEntry original = new JournalEntry();
        original.setId(1L);
        original.setSlipNo("GL759-AUDITED");
        original.setSlipDate(LocalDate.of(2026, 9, 25));
        original.setAccountingDate(LocalDate.of(2026, 9, 25));
        original.setDescription("audited compatibility source");
        original.setCurrencyCode("KRW");
        original.setCreatedBy("maker");
        original.addDetail(detail(JournalSide.DEBIT, "10100"));
        original.addDetail(detail(JournalSide.CREDIT, "40100"));
        original.initializeDraft();
        try {
            JournalEntry.class.getMethod("requestApproval", String.class).invoke(original, "maker");
        } catch (NoSuchMethodException auditedLifecycle) {
            // Audited a97d10ab approved directly from DRAFT; current code requires REQUESTED first.
        }
        original.approve("checker");
        original.post("poster");
        return original;
    }

    private static JournalDetail detail(JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        return detail;
    }
}
