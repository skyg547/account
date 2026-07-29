package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 승인된 거래처 변경 요청을 실제 SCD2 유즈케이스에 연결합니다.
 */
@Service
@RequiredArgsConstructor
public class BusinessPartnerMasterDataChangeApplier implements MasterDataChangeApplier {

    private final BusinessPartnerUseCase businessPartnerUseCase;
    private final MasterDataChangePayloadDecoder payloadDecoder;

    @Override
    public MasterDataType targetType() {
        return MasterDataType.BUSINESS_PARTNER;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() != MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            command(request).toDomain();
        }
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> businessPartnerUseCase.createBusinessPartner(command(request));
            case UPDATE -> businessPartnerUseCase.updateBusinessPartner(currentId(request), command(request));
            case DEACTIVATE -> businessPartnerUseCase.deleteBusinessPartner(
                    currentId(request), request.getEffectiveDate());
        }
    }

    private BusinessPartnerCommand command(MasterDataChangeRequest request) {
        BusinessPartnerCommand payload = MasterDataChangeApplierSupport.decode(
                request, payloadDecoder, BusinessPartnerCommand.class);
        String code = MasterDataChangeApplierSupport.targetKey(request, payload.businessPartnerCode());
        return new BusinessPartnerCommand(
                code,
                payload.businessPartnerName(),
                payload.registrationNumber(),
                payload.ceoName(),
                payload.businessType(),
                payload.businessItem(),
                payload.partnerType(),
                payload.useYn(),
                payload.kycStatus(),
                payload.riskRating(),
                request.getEffectiveDate(),
                payload.validTo());
    }

    private Long currentId(MasterDataChangeRequest request) {
        BusinessPartner current = businessPartnerUseCase.getBusinessPartnerByCode(request.getTargetKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active business partner not found. Code: " + request.getTargetKey()));
        return MasterDataChangeApplierSupport.requirePersistentId(current.getId(), request);
    }
}
