package com.ho.account.contracts.journal;

import java.time.LocalDate;

public class JournalSummary {
    private Long id;
    private String slipNo;
    private LocalDate slipDate;
    private LocalDate accountingDate;
    private String description;
    private String entryType;
    private String status;
    private String currencyCode;
    private String lineageSourceType;
    private String lineageSourceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlipNo() {
        return slipNo;
    }

    public void setSlipNo(String slipNo) {
        this.slipNo = slipNo;
    }

    public LocalDate getSlipDate() {
        return slipDate;
    }

    public void setSlipDate(LocalDate slipDate) {
        this.slipDate = slipDate;
    }

    public LocalDate getAccountingDate() {
        return accountingDate;
    }

    public void setAccountingDate(LocalDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getLineageSourceType() {
        return lineageSourceType;
    }

    public void setLineageSourceType(String lineageSourceType) {
        this.lineageSourceType = lineageSourceType;
    }

    public String getLineageSourceId() {
        return lineageSourceId;
    }

    public void setLineageSourceId(String lineageSourceId) {
        this.lineageSourceId = lineageSourceId;
    }
}
