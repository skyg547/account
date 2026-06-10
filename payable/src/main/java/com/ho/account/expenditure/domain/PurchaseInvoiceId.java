package com.ho.account.expenditure.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * PurchaseInvoice 엔티티의 복합 키를 정의하는 클래스입니다.
 *
 * <p>초보자용 설명: 같은 인보이스 번호라도 공급업체가 다르면 다른 청구서일 수 있습니다.
 * 그래서 인보이스 번호와 공급업체 코드를 함께 묶어 하나의 업무 식별자로 사용합니다.
 * 공급업체 엔티티를 직접 연결하지 않고 코드만 보관하면 master-data 모듈 내부 구조와
 * payable 도메인을 느슨하게 분리할 수 있습니다.</p>
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
