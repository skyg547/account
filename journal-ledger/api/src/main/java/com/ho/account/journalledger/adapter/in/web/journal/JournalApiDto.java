package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.domain.journal.domain.JournalActor;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class JournalApiDto {

    private static final String MANUAL_SOURCE_TYPE = "MANUAL_JOURNAL";

    private JournalApiDto() {
    }

    record CreateRequest(
            @NotNull LocalDate slipDate,
            LocalDate accountingDate,
            @NotBlank String description,
            String entryType,
            String currencyCode,
            @Positive @Digits(integer = 11, fraction = 8) BigDecimal exchangeRate,
            String lineageSourceType,
            String lineageSourceId,
            @NotEmpty List<@Valid LineRequest> lines) {

        JournalCreationIdentity creationIdentity(String actor, String operationKey) {
            String maker = requireActor(actor);
            boolean hasExplicitLineage = hasText(lineageSourceType) || hasText(lineageSourceId);
            return hasExplicitLineage
                    ? new JournalCreationIdentity(lineageSourceType, lineageSourceId, operationKey)
                    : new JournalCreationIdentity(MANUAL_SOURCE_TYPE, maker, operationKey);
        }

        JournalEntry toDomain(String actor, JournalCreationIdentity identity) {
            JournalEntry entry = new JournalEntry();
            entry.setSlipDate(slipDate);
            entry.setAccountingDate(accountingDate);
            entry.setDescription(description);
            entry.setEntryType(entryType);
            entry.setCurrencyCode(currencyCode);
            entry.setExchangeRate(exchangeRate);
            // Persist exactly the lineage used by the durable operation claim.
            entry.setLineageSourceType(identity.sourceType());
            entry.setLineageSourceId(identity.sourceId());
            entry.setCreatedBy(requireActor(actor));
            lines.forEach(line -> entry.addDetail(line.toDomain(actor)));
            return entry;
        }
    }

    record LineRequest(
            @NotNull JournalSide side,
            @NotBlank String accountCode,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal baseAmount,
            String departmentCode,
            String businessPartnerCode,
            String description) {

        JournalDetail toDomain(String actor) {
            JournalDetail detail = new JournalDetail();
            detail.setSide(side);
            detail.setAccountCode(accountCode);
            detail.setAmount(amount);
            detail.setBaseAmount(baseAmount);
            detail.setDepartmentCode(departmentCode);
            detail.setBusinessPartnerCode(businessPartnerCode);
            detail.setDetailDescription(description);
            detail.setAuditUser(requireActor(actor));
            return detail;
        }
    }

    record EventRequest(@NotNull Map<String, Object> eventData) {
        JournalCreationIdentity creationIdentity(String headerOperationKey) {
            String sourceType = firstStableScalar(
                    eventData, "lineageSourceType", "eventType", "ruleCode");
            String sourceId = firstStableScalar(
                    eventData, "lineageSourceId", "eventId", "sourceId", "documentId",
                    "assetCode", "contractNo");
            String operationKey = headerOperationKey != null
                    ? headerOperationKey
                    : firstStableScalar(eventData, "journalOperationKey", "operationKey");
            return new JournalCreationIdentity(sourceType, sourceId, operationKey);
        }

        Map<String, Object> eventDataWithActor(String actor, JournalCreationIdentity identity) {
            Map<String, Object> enriched = new java.util.HashMap<>(eventData);
            // Payload identity is untrusted; the Gateway-authenticated principal always wins.
            enriched.put("createdBy", requireActor(actor));
            enriched.put("auditUser", requireActor(actor));
            // Rule generation must persist the same lineage protected by the operation claim.
            enriched.put("lineageSourceType", identity.sourceType());
            enriched.put("lineageSourceId", identity.sourceId());
            // The header wins over both payload key spellings. Normalize both spellings so
            // rule expressions using the alias still work and equivalent retries hash alike.
            enriched.put("operationKey", identity.operationKey());
            enriched.put("journalOperationKey", identity.operationKey());
            return enriched;
        }
    }

    record View(
            Long id,
            String slipNo,
            LocalDate slipDate,
            LocalDate accountingDate,
            String description,
            String status,
            String entryType,
            String currencyCode,
            BigDecimal exchangeRate,
            String createdBy,
            String approvedBy,
            String auditUser,
            String lineageSourceType,
            String lineageSourceId,
            List<LineView> lines) {

        static View from(JournalEntry entry) {
            return new View(
                    entry.getId(),
                    entry.getSlipNo(),
                    entry.getSlipDate(),
                    entry.getAccountingDate(),
                    entry.getDescription(),
                    entry.getStatus() == null ? null : entry.getStatus().name(),
                    entry.getEntryType(),
                    entry.getCurrencyCode(),
                    entry.getExchangeRate(),
                    entry.getCreatedBy(),
                    entry.getApprovedBy(),
                    entry.getAuditUser(),
                    entry.getLineageSourceType(),
                    entry.getLineageSourceId(),
                    entry.getDetails().stream().map(LineView::from).toList());
        }
    }

    record LineView(
            Long id,
            String side,
            String accountCode,
            BigDecimal amount,
            BigDecimal baseAmount,
            String departmentCode,
            String businessPartnerCode,
            String description) {

        static LineView from(JournalDetail detail) {
            return new LineView(
                    detail.getId(),
                    detail.getSide() == null ? null : detail.getSide().name(),
                    detail.getAccountCode(),
                    detail.getAmount(),
                    detail.getBaseAmount(),
                    detail.getDepartmentCode(),
                    detail.getBusinessPartnerCode(),
                    detail.getDetailDescription());
        }
    }

    private static String requireActor(String actor) {
        return JournalActor.canonicalize(actor);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstStableScalar(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value == null) {
                continue;
            }
            if (!(value instanceof String) && !(value instanceof Number)) {
                throw new IllegalArgumentException(key + " must be a string or number");
            }
            String text = value.toString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
