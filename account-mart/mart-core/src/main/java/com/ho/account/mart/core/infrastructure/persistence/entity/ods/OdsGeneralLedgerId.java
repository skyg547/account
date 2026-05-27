package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * OdsGeneralLedger 엔티티의 복합 기본키(Composite PK) 클래스입니다.
 * 기준일자, 계정과목, 점포, 통화의 조합으로 유니크한 원장 데이터를 식별합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OdsGeneralLedgerId implements Serializable {
    private LocalDate baseDate;
    private String glCode;
    private String branchCode;
    private String currency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OdsGeneralLedgerId that = (OdsGeneralLedgerId) o;
        return Objects.equals(baseDate, that.baseDate) &&
               Objects.equals(glCode, that.glCode) &&
               Objects.equals(branchCode, that.branchCode) &&
               Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseDate, glCode, branchCode, currency);
    }
}
