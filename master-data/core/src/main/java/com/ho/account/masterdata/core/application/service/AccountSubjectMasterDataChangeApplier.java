package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import java.time.LocalDate;
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
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;

    @Override
    public MasterDataType targetType() {
        return MasterDataType.ACCOUNT_SUBJECT;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() != MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            validatedCommand(request);
        }
    }

    private AccountSubjectCommand validatedCommand(MasterDataChangeRequest request) {
        AccountSubjectCommand command = command(request);
        // CREATE/UPDATE 모두 새 이름이 필요하지만 category/balanceType의 기존 저장 기본값은 유지합니다.
        if (command.name() == null) {
            throw new IllegalArgumentException("Account subject name is required.");
        }
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
        return command;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> accountSubjectUseCase.createAccountSubject(validatedCommand(request));
            case UPDATE -> {
                AccountSubjectCommand command = validatedCommand(request);
                AccountSubject current = current(request);
                MasterDataChangeApplierSupport.requireCurrentUpdateWindow(
                        current.getValidTo(), command.validFrom(), command.validTo());
                accountSubjectUseCase.updateAccountSubject(request.getTargetKey(), command);
            }
            case DEACTIVATE -> {
                current(request);
                accountSubjectUseCase.deactivateAccountSubject(request.getTargetKey(), request.getEffectiveDate());
            }
        }
    }

    private AccountSubject current(MasterDataChangeRequest request) {
        // 이력 수는 DEACTIVATE 후에도 같으므로 잠금 뒤 실제 반영할 현재 버전도 확인합니다.
        return accountSubjectPersistencePort.findByCodeForUpdate(request.getTargetKey())
                .orElseThrow(() -> new MasterDataVersionConflictException(
                        "Active account subject no longer exists. Code: " + request.getTargetKey()));
    }

    private AccountSubjectCommand command(MasterDataChangeRequest request) {
        AccountSubjectCommand payload = MasterDataChangeApplierSupport.decode(
                request, payloadDecoder, AccountSubjectCommand.class);
        if (payload == null) {
            throw new IllegalArgumentException("Account subject change payload must not be null.");
        }
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
