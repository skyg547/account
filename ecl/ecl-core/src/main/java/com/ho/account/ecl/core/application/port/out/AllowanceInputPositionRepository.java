package com.ho.account.ecl.core.application.port.out;

import com.ho.account.shared.finance.entity.AllowanceInputPosition;
import java.time.LocalDate;
import java.util.List;

/**
 * [Port] 통합 대손충당금 입력 포지션 데이터소스 인터페이스.
 */
public interface AllowanceInputPositionRepository {
    List<AllowanceInputPosition> findByBaseDt(LocalDate baseDt);
}


