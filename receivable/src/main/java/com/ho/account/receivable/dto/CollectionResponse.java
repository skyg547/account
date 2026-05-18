package com.ho.account.receivable.dto;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CollectionResponse {

    private Long id;
    private LocalDate collectionDate;
    private String customerCode;
    private BigDecimal amount;
    private String bankAccount;
    private String virtualAccount;
    private String referenceNo;
    private CollectionStatus status;
    private LocalDateTime createdAt;

    public static CollectionResponse fromEntity(Collection collection) {
        CollectionResponse response = new CollectionResponse();
        response.setId(collection.getId());
        response.setCollectionDate(collection.getCollectionDate());
        response.setCustomerCode(collection.getCustomerCode());
        response.setAmount(collection.getAmount());
        response.setBankAccount(collection.getBankAccount());
        response.setVirtualAccount(collection.getVirtualAccount());
        response.setReferenceNo(collection.getReferenceNo());
        response.setStatus(collection.getStatus());
        response.setCreatedAt(collection.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getVirtualAccount() {
        return virtualAccount;
    }

    public void setVirtualAccount(String virtualAccount) {
        this.virtualAccount = virtualAccount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public CollectionStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
