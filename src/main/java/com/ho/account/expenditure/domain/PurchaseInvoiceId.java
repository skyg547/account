package com.ho.account.expenditure.domain;

import com.ho.account.basic.domain.BusinessPartner;
import java.io.Serializable;
import java.util.Objects;

/**
 * PurchaseInvoice 엔티티의 복합 키를 정의하는 클래스.
 * invoiceNo와 vendor(BusinessPartner)를 조합하여 유니크한 인보이스를 식별합니다.
 */
public class PurchaseInvoiceId implements Serializable {

    private String invoiceNo;
    private String vendor; // BusinessPartner's businessPartnerCode

    // Default constructor
    public PurchaseInvoiceId() {}

    public PurchaseInvoiceId(String invoiceNo, String vendor) {
        this.invoiceNo = invoiceNo;
        this.vendor = vendor;
    }

    // Getter
    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getVendor() {
        return vendor;
    }

    // Setters (optional, but good practice for JPA)
    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseInvoiceId that = (PurchaseInvoiceId) o;
        return Objects.equals(invoiceNo, that.invoiceNo) &&
               Objects.equals(vendor, that.vendor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceNo, vendor);
    }
}
