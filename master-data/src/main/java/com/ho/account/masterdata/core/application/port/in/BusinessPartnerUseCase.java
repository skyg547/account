package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 거래처 관리 유즈케이스 인터페이스
 *
 * <p>비즈니스 로직을 실행하기 위한 인바운드 포트(Inbound Port)를 정의합니다.
 * 외부 시스템(REST API, Batch, Kafka 등)은 이 인터페이스를 통해 시스템을 제어합니다.</p>
 */
public interface BusinessPartnerUseCase {

    BusinessPartner createBusinessPartner(BusinessPartnerCommand command);

    List<BusinessPartner> getAllBusinessPartners();

    List<BusinessPartner> getActiveBusinessPartners();

    Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode);

    List<BusinessPartner> searchBusinessPartnersByName(String name);

    BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command);

    void deleteBusinessPartner(Long id);

    /**
     * 승인 워크플로에서 정한 종료일로 현재 SCD2 버전을 비활성화합니다.
     */
    void deleteBusinessPartner(Long id, LocalDate effectiveDate);
}