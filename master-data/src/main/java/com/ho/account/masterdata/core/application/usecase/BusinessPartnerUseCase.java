package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import java.util.List;
import java.util.Optional;

/**
 * 거래처 마스터 입력 포트입니다.
 *
 * <p>Controller 같은 inbound adapter는 이 인터페이스만 호출합니다. 이렇게 두면 REST가 아닌
 * Kafka, batch, CLI가 들어오더라도 같은 유스케이스 규칙을 재사용할 수 있습니다.</p>
 */
public interface BusinessPartnerUseCase {

    BusinessPartner createBusinessPartner(BusinessPartnerCommand command);

    List<BusinessPartner> getAllBusinessPartners();

    List<BusinessPartner> getActiveBusinessPartners();

    Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode);

    List<BusinessPartner> searchBusinessPartnersByName(String name);

    BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command);

    void deleteBusinessPartner(Long id);
}
