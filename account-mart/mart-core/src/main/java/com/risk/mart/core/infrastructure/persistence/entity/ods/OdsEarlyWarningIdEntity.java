package com.risk.mart.core.infrastructure.persistence.entity.ods;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OdsEarlyWarningIdEntity implements Serializable {
    private LocalDate baseDate;
    private String customerCode;
}
