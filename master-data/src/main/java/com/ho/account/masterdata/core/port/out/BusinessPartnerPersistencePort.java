package com.ho.account.masterdata.core.port.out;

import com.ho.account.basic.domain.BusinessPartner;
import java.util.List;
import java.util.Optional;

/**
 * 거래처 저장소 출력 포트입니다.
 *
 * <p>거래처는 AP/AR/자금/은행 원천에서 모두 참조되는 기준정보입니다. 그래서 application
 * 계층은 "어디에 저장되는지"보다 "어떤 조회와 저장이 필요한지"만 정의합니다.</p>
 */
public interface BusinessPartnerPersistencePort {

    boolean existsByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findById(Long id);

    List<BusinessPartner> findAll();

    List<BusinessPartner> findByUseYnTrue();

    List<BusinessPartner> findByBusinessPartnerNameContaining(String name);

    BusinessPartner save(BusinessPartner businessPartner);
}
