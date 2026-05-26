package com.risk.mart.core.infrastructure.persistence.entity.ods;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * [여신 도메인] 조기경보 엔티티 복합키 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OdsEarlyWarningId implements Serializable {
    private LocalDate baseDate;
    private String customerCode;
}
