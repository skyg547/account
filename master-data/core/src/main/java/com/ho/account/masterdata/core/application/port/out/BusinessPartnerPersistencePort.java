package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output port for business-partner persistence.
 *
 * <p>AP, AR, treasury, and other modules resolve counterparty reference data
 * through this application-layer contract instead of repository details. 포트가 순수 도메인 타입만
 * 노출하므로 application service는 JPA entity나 query 문법을 알 필요가 없습니다.</p>
 */
public interface BusinessPartnerPersistencePort {

    boolean existsByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode);

    /**
     * 지정 기준일에 유효했던 거래처 버전을 조회합니다.
     *
     * <p>SCD2 이력은 "오늘의 거래처"와 "전표 작성 당시 거래처"가 다를 수 있습니다.
     * 기준일을 포트 계약에 명시하면 adapter가 데이터베이스에서 정확한 버전 한 건을 선택하고,
     * 호출자가 전체 이력을 메모리에서 임의로 필터링하지 않게 됩니다.</p>
     */
    Optional<BusinessPartner> findEffectiveByBusinessPartnerCode(
            String businessPartnerCode,
            LocalDate effectiveDate);

    Optional<BusinessPartner> findById(Long id);

    List<BusinessPartner> findAll();

    List<BusinessPartner> findByUseYnTrue();

    List<BusinessPartner> searchActiveByName(String name, LocalDate asOfDate);

    BusinessPartner save(BusinessPartner businessPartner);
}
