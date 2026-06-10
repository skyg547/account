package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class AllowanceInputPositionJpaId implements Serializable {
    private LocalDate baseDt;
    private String accNo;

    public AllowanceInputPositionJpaId() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AllowanceInputPositionJpaId that)) return false;
        return Objects.equals(baseDt, that.baseDt) && Objects.equals(accNo, that.accNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseDt, accNo);
    }
}
