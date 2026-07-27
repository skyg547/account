package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 승인된 계정과목 변경 요청을 실제 SCD2 유즈케이스에 연결합니다.
 */
@Service
@RequiredArgsConstructor
public class AccountSubjectMasterDataChangeApplier implements MasterDataChangeApplier {

    private final AccountSubjectUseCase accountSubjectUseCase;
    private final MasterDataChangePayloadDecoder payloadDecoder;

    @Override
    public MasterDataType targetType() {
        return MasterDataType.ACCOUNT_SUBJECT;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> accountSubjectUseCase.createAccountSubject(command(request));
            case UPDATE -> accountSubjectUseCase.updateAccountSubject(request.getTargetKey(), command(request));
            case DEACTIVATE -> accountSubjectUseCase.deactivateAccountSubject(
                    request.getTargetKey(), request.getEffectiveDate());
        }
    }

    private AccountSubjectCommand command(MasterDataChangeRequest request) {
        AccountSubjectCommand payload = MasterDataChangeApplierSupport.decode(
                request, payloadDecoder, AccountSubjectCommand.class);
        String code = MasterDataChangeApplierSupport.targetKey(request, payload.code());
        return new AccountSubjectCommand(
                code,
                payload.name(),
                payload.parentCode(),
                payload.category(),
                payload.balanceType(),
                payload.reportLine(),
                payload.unsettled(),
                payload.fixedAsset(),
                request.getEffectiveDate(),
                payload.validTo());
    }
}