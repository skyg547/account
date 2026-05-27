package com.ho.account.ecl.core.domain.result;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite Primary Key for CrRiskResult to match partitioned SQL schema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrRiskResultId implements Serializable {
    private LocalDate baseDate;
    private Long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrRiskResultId that = (CrRiskResultId) o;
        return Objects.equals(baseDate, that.baseDate) && 
               Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseDate, id);
    }
}
