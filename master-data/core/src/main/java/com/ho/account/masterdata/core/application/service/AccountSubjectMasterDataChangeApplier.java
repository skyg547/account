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
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE
                && command.clearRegulatoryMappingCode()) {
            throw new IllegalArgumentException("Cannot clear regulatory mapping code when creating an account subject.");
        }
        // Both requests need a name; other omitted business fields inherit the current UPDATE version.
        if (command.name() == null) {
            throw new IllegalArgumentException("Account subject name is required.");
        }
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
        validateProspectiveClassification(request, command);
        return command;
    }

    private void validateProspectiveClassification(MasterDataChangeRequest request, AccountSubjectCommand command) {
        AccountSubject candidate;
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE) {
            candidate = command.toEntity();
        } else {
            // Resolve omitted fields against the live version before approval; apply rechecks its own snapshot.
            AccountSubject current = accountSubjectUseCase.findAccountSubjectByCode(request.getTargetKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Active account subject not found: " + request.getTargetKey()));
            candidate = current.successor();
            if (command.category() != null) {
                candidate.setCategory(command.category());
            }
            if (command.accountType() != null) {
                candidate.setAccountType(command.accountType());
            }
        }
        candidate.normalizeAndValidateClassification();
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> accountSubjectUseCase.createAccountSubject(command(request));
            case UPDATE -> {
                AccountSubjectCommand command = command(request);
                MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                        command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
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
        // Omitted fields preserve the current value; explicit null must not silently do the same.
        if (payloadDecoder.hasExplicitNullField(request, "accountType")) {
            throw new IllegalArgumentException("Account subject accountType must not be null.");
        }
        if (payloadDecoder.hasExplicitNullField(request, "regulatoryMappingCode")) {
            throw new IllegalArgumentException("Account subject regulatoryMappingCode must not be null.");
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
                payload.validTo(),
                payload.accountType(),
                payload.regulatoryMappingCode(),
                payload.clearRegulatoryMappingCode());
    }
}
