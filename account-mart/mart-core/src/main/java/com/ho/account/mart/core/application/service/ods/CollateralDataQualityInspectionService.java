package com.ho.account.mart.core.application.service.ods;

import com.ho.account.mart.core.application.port.out.OdsApartCollDetailRepository;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.audit.processor.CollateralDataQualityProcessor;
import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * [Application Service] 담보 DQ 검사 유즈케이스.
 *
 * <p>초보자 설명: Batch나 API는 이 서비스를 호출하기만 합니다. 이 서비스는 필요한 원천 상세를
 * port로 조회하고, 실제 오류 판단은 domain processor에 맡깁니다. 이렇게 나누면 Spring Batch,
 * JPA, 업무 규칙이 한 클래스에 뒤섞이지 않습니다.</p>
 */
@Service
@RequiredArgsConstructor
public class CollateralDataQualityInspectionService {

    private final OdsApartCollDetailRepository apartmentDetailRepository;
    private final CollateralDataQualityProcessor processor;

    @Nullable
    public OdsDqAudit inspect(@NonNull OdsCollateralMst collateral, @NonNull LocalDate baseDate) {
        Optional<OdsApartCollDetail> apartmentDetail = processor.requiresApartmentDetail(collateral)
                ? apartmentDetailRepository.findByCollateralId(collateral.getCollateralNo())
                : Optional.empty();
        return processor.inspect(collateral, apartmentDetail, baseDate);
    }
}
