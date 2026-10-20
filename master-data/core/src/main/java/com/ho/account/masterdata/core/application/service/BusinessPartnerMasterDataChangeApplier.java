package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
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
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    @Override
    public MasterDataType targetType() {
        return MasterDataType.BUSINESS_PARTNER;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() != MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            validatedCommand(request);
        }
    }

    private BusinessPartnerCommand validatedCommand(MasterDataChangeRequest request) {
        BusinessPartnerCommand command = command(request);
        // Reject malformed dates before an UPDATE lookup can mask the payload error as missing state.
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : BusinessPartner.OPEN_ENDED_VALID_TO);
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE) {
            command.toDomain();
        } else {
            BusinessPartner current = businessPartnerUseCase.getBusinessPartnerByCode(request.getTargetKey())
                    .orElseThrow(() -> new MasterDataVersionConflictException("Active business partner no longer exists."));
            // Merge omitted UPDATE fields before approval; explicit blanks still reach the domain factory.
            BusinessPartner.create(current.getBusinessPartnerCode(),
                    command.businessPartnerName() != null ? command.businessPartnerName() : current.getBusinessPartnerName(),
                    command.registrationNumber() != null ? command.registrationNumber() : current.getRegistrationNumber(),
                    command.ceoName() != null ? command.ceoName() : current.getCeoName(),
                    command.businessType() != null ? command.businessType() : current.getBusinessType(),
                    command.businessItem() != null ? command.businessItem() : current.getBusinessItem(),
                    command.partnerType() != null ? command.partnerType() : current.getPartnerType(),
                    command.useYn() != null ? command.useYn() : current.getUseYn(),
                    command.kycStatus() != null ? command.kycStatus() : current.getKycStatus(),
                    command.riskRating() != null ? command.riskRating() : current.getRiskRating(),
                    command.validFrom(), command.validTo());
        }
        return command;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        validate(request);
        switch (request.getChangeType()) {
            case CREATE -> businessPartnerUseCase.createBusinessPartner(validatedCommand(request));
            case UPDATE -> {
                BusinessPartnerCommand command = validatedCommand(request);
                BusinessPartner current = current(request);
                MasterDataChangeApplierSupport.requireCurrentUpdateWindow(
                        current.getValidTo(), command.validFrom(), command.validTo());
                businessPartnerUseCase.updateBusinessPartner(
                        MasterDataChangeApplierSupport.requirePersistentId(current.getId(), request), command);
            }
            case DEACTIVATE -> businessPartnerUseCase.deleteBusinessPartner(
                    MasterDataChangeApplierSupport.requirePersistentId(current(request).getId(), request),
                    request.getEffectiveDate());
        }
    }

    private BusinessPartnerCommand command(MasterDataChangeRequest request) {
        BusinessPartnerCommand payload = MasterDataChangeApplierSupport.decode(
                request, payloadDecoder, BusinessPartnerCommand.class);
        if (payload == null) {
            throw new IllegalArgumentException("Business partner change payload must not be null.");
        }
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

    private BusinessPartner current(MasterDataChangeRequest request) {
        return businessPartnerPersistencePort.findByBusinessPartnerCodeForUpdate(request.getTargetKey())
                .orElseThrow(() -> new MasterDataVersionConflictException(
                        "Active business partner not found. Code: " + request.getTargetKey()));
    }
}
