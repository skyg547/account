package com.ho.account.expenditure.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.io.Serializable;
import java.util.Objects;

/**
 * PurchaseInvoice ?”í‹°?°ì˜ ë³µí•© ?¤ë? ?•ì˜?˜ëŠ” ?´ë˜??
 * invoiceNo?€ vendor(BusinessPartner)ë¥?ì¡°í•©?˜ì—¬ ? ë‹ˆ?¬í•œ ?¸ë³´?´ìŠ¤ë¥??ë³„?©ë‹ˆ??
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
