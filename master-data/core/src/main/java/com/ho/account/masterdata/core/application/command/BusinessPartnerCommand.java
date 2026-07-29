package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;

/**
 * 거래처 등록/수정 요청을 위한 Application Command 클래스입니다.
 *
 * <p>REST 요청 DTO로부터 변환되어 서비스 계층으로 전달되며, API 스펙과
 * 도메인 모델 사이의 완충 역할을 수행합니다. Command는 데이터를 운반할 뿐이고,
 * 실제 필수값·기본값·유효기간 판단은 모든 입력 경로가 공유하는 도메인 factory가 담당합니다.</p>
 */
public record BusinessPartnerCommand(
        String businessPartnerCode,
        String businessPartnerName,
        String registrationNumber,
        String ceoName,
        String businessType,
        String businessItem,
        BusinessPartner.PartnerType partnerType,
        Boolean useYn,
        BusinessPartner.KycStatus kycStatus,
        BusinessPartner.RiskRating riskRating,
        LocalDate validFrom,
        LocalDate validTo) {

    /**
     * 저장 엔티티가 아니라 순수 도메인 aggregate를 생성합니다.
     */
    public BusinessPartner toDomain() {
        return BusinessPartner.create(
                businessPartnerCode,
                businessPartnerName,
                registrationNumber,
                ceoName,
                businessType,
                businessItem,
                partnerType,
                useYn,
                kycStatus,
                riskRating,
                validFrom,
                validTo);
    }

    /**
     * 이전 호출자와의 소스 호환성을 위한 별칭입니다.
     *
     * @deprecated 도메인과 영속성 엔티티가 분리되었으므로 {@link #toDomain()}을 사용하세요.
     */
    @Deprecated
    public BusinessPartner toEntity() {
        return toDomain();
    }
}
