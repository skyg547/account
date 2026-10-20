package com.ho.account.closing.application.port.out;

/** Expected durable lineage for a deterministic Closing slip, including zero-delta groups. */
public record ClosingJournalLineage(String slipNo, java.time.LocalDate accountingDate,
                                    String currencyCode, String snapshotReference,
                                    String operationKey, boolean noAdjustment) { }
