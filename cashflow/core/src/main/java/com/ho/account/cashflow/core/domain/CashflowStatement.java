package com.ho.account.cashflow.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class CashflowStatement {

    private final String statementId;
    private final int fiscalYear;
    private final int fiscalPeriod;
    private final CashflowMethod method;
    private final String currency;
    private final LocalDateTime generatedAt;
    private final List<CashflowLineItem> lineItems;
    private final BigDecimal totalOperating;
    private final BigDecimal totalInvesting;
    private final BigDecimal totalFinancing;
    private final BigDecimal netCashflow;
    private final BigDecimal beginningCash;
    private final BigDecimal endingCash;

    private CashflowStatement(
            String statementId,
            int fiscalYear,
            int fiscalPeriod,
            CashflowMethod method,
            String currency,
            LocalDateTime generatedAt,
            List<CashflowLineItem> lineItems,
            BigDecimal totalOperating,
            BigDecimal totalInvesting,
            BigDecimal totalFinancing,
            BigDecimal netCashflow,
            BigDecimal beginningCash,
            BigDecimal endingCash) {
        this.statementId = requireText(statementId, "statementId");
        if (fiscalYear < 1900 || fiscalYear > 9999) {
            throw new IllegalArgumentException("fiscalYear must be between 1900 and 9999");
        }
        if (fiscalPeriod < 1 || fiscalPeriod > 12) {
            throw new IllegalArgumentException("fiscalPeriod must be between 1 and 12");
        }
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.currency = CashflowAmounts.normalizeCurrency(currency);
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        this.lineItems = List.copyOf(Objects.requireNonNull(lineItems, "lineItems must not be null"));
        validateLineItemCurrencies(this.lineItems, this.currency);
        this.totalOperating = CashflowAmounts.normalize(totalOperating, "totalOperating");
        this.totalInvesting = CashflowAmounts.normalize(totalInvesting, "totalInvesting");
        this.totalFinancing = CashflowAmounts.normalize(totalFinancing, "totalFinancing");
        this.netCashflow = CashflowAmounts.normalize(netCashflow, "netCashflow");
        this.beginningCash = CashflowAmounts.normalize(beginningCash, "beginningCash");
        this.endingCash = CashflowAmounts.normalize(endingCash, "endingCash");
        validateInvariants();
    }

    public static CashflowStatement generate(
            String statementId,
            int fiscalYear,
            int fiscalPeriod,
            CashflowMethod method,
            String currency,
            LocalDateTime generatedAt,
            List<CashflowLineItem> lineItems,
            BigDecimal beginningCash) {
        Objects.requireNonNull(lineItems, "lineItems must not be null");
        BigDecimal operating = totalFor(lineItems, CashflowActivity.OPERATING);
        BigDecimal investing = totalFor(lineItems, CashflowActivity.INVESTING);
        BigDecimal financing = totalFor(lineItems, CashflowActivity.FINANCING);
        BigDecimal normalizedBeginningCash = CashflowAmounts.normalize(beginningCash, "beginningCash");
        BigDecimal net = operating.add(investing).add(financing);
        BigDecimal ending = normalizedBeginningCash.add(net);
        return new CashflowStatement(
                statementId, fiscalYear, fiscalPeriod, method, currency, generatedAt, lineItems,
                operating, investing, financing, net, normalizedBeginningCash, ending);
    }

    public static CashflowStatement reconstitute(
            String statementId,
            int fiscalYear,
            int fiscalPeriod,
            CashflowMethod method,
            String currency,
            LocalDateTime generatedAt,
            List<CashflowLineItem> lineItems,
            BigDecimal totalOperating,
            BigDecimal totalInvesting,
            BigDecimal totalFinancing,
            BigDecimal netCashflow,
            BigDecimal beginningCash,
            BigDecimal endingCash) {
        return new CashflowStatement(
                statementId, fiscalYear, fiscalPeriod, method, currency, generatedAt, lineItems,
                totalOperating, totalInvesting, totalFinancing, netCashflow, beginningCash, endingCash);
    }

    private static BigDecimal totalFor(List<CashflowLineItem> lineItems, CashflowActivity activity) {
        return lineItems.stream()
                .filter(item -> item.activity() == activity)
                .map(CashflowLineItem::amount)
                .reduce(CashflowAmounts.ZERO, BigDecimal::add)
                .setScale(CashflowAmounts.SCALE);
    }

    private static void validateLineItemCurrencies(List<CashflowLineItem> lineItems, String currency) {
        boolean mismatch = lineItems.stream().anyMatch(item -> !item.currency().equals(currency));
        if (mismatch) {
            throw new IllegalArgumentException("all line item currencies must match statement currency");
        }
    }

    private void validateInvariants() {
        // Persisted totals are checked independently of line items so corrupted snapshots cannot be loaded silently.
        if (totalOperating.compareTo(totalFor(lineItems, CashflowActivity.OPERATING)) != 0
                || totalInvesting.compareTo(totalFor(lineItems, CashflowActivity.INVESTING)) != 0
                || totalFinancing.compareTo(totalFor(lineItems, CashflowActivity.FINANCING)) != 0) {
            throw new IllegalArgumentException("activity totals must equal the sum of their line items");
        }
        BigDecimal expectedNet = totalOperating.add(totalInvesting).add(totalFinancing);
        if (netCashflow.compareTo(expectedNet) != 0) {
            throw new IllegalArgumentException("netCashflow must equal operating + investing + financing totals");
        }
        if (endingCash.compareTo(beginningCash.add(netCashflow)) != 0) {
            throw new IllegalArgumentException("endingCash must equal beginningCash + netCashflow");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public String getStatementId() { return statementId; }
    public int getFiscalYear() { return fiscalYear; }
    public int getFiscalPeriod() { return fiscalPeriod; }
    public CashflowMethod getMethod() { return method; }
    public String getCurrency() { return currency; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public List<CashflowLineItem> getLineItems() { return lineItems; }
    public BigDecimal getTotalOperating() { return totalOperating; }
    public BigDecimal getTotalInvesting() { return totalInvesting; }
    public BigDecimal getTotalFinancing() { return totalFinancing; }
    public BigDecimal getNetCashflow() { return netCashflow; }
    public BigDecimal getBeginningCash() { return beginningCash; }
    public BigDecimal getEndingCash() { return endingCash; }
}
