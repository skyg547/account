package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * OdsBalanceHist 엔티티의 논리적 복합키(Logical Composite PK) 클래스입니다.
 * 기준일자와 계좌번호의 조합으로 특정 시점의 계좌 잔액을 식별합니다.
 * ODS 잔액 이력은 기준일자와 계좌번호의 복합키를 사용합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OdsBalanceHistId implements Serializable {
    private LocalDate baseDate;
    private String accountNo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OdsBalanceHistId that = (OdsBalanceHistId) o;
        return Objects.equals(baseDate, that.baseDate) && 
               Objects.equals(accountNo, that.accountNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseDate, accountNo);
    }
}
