package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ë‰???‚Âƒì„????ë’ª?³Â??ë’ª ??•í‰¬??¼ì—¯??ˆë–.
 *
 * <p>????•í‰¬??»ë’— ‚ÂƒìŒ?‚ï????¹ê¹­ ????¿Â?±Ñ‹ë???ˆë–. ??¼ì £ ?¾©?™æ‡°??„ê³•?’ï?row??›ë¶½????’ë¾½??
 * ?????¿ê»Œ???¸ìŠœ??ë’— ‚ê¾¨?application service ??’— batch›Â ?????ë£„??ºê¾¨???¸ë•²??</p>
 */
@Service
@Transactional
public class MasterDataChangeRequestService implements MasterDataChangeRequestUseCase {

    private final MasterDataChangeRequestPersistencePort persistencePort;
    private final MasterDataChangeApplier changeApplier;

    public MasterDataChangeRequestService(MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataChangeApplier changeApplier) {
        this.persistencePort = persistencePort;
        this.changeApplier = changeApplier;
    }

    @Override
    public MasterDataChangeRequest requestChange(MasterDataChangeRequestCommand command) {
        MasterDataChangeRequest request = new MasterDataChangeRequest(
                command.targetType(),
                command.targetKey(),
                command.changeType(),
                command.effectiveDate(),
                command.requestedVersion(),
                command.requestedBy(),
                command.reason(),
                command.payloadJson());
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest approve(Long requestId, String approver) {
        MasterDataChangeRequest request = findRequired(requestId);
        request.approve(approver);
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest reject(Long requestId, String approver, String reason) {
        MasterDataChangeRequest request = findRequired(requestId);
        request.reject(approver, reason);
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest markApplied(Long requestId) {
        MasterDataChangeRequest request = findRequired(requestId);
        request.markApplied();
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest applyApprovedChange(Long requestId) {
        MasterDataChangeRequest request = findRequired(requestId);
        if (!request.isReadyToApply(LocalDate.now())) {
            throw new IllegalStateException("????¹ê¹­??¿í??¸ìŠœ??±ì”  ?˜’???¿ê»Œ??¸ìŠœ??????‰ë’¿??ˆë–.");
        }
        changeApplier.apply(request);
        request.markApplied();
        return persistencePort.save(request);
    }

    @Override
    public List<MasterDataChangeRequest> applyDueApprovedChanges() {
        LocalDate today = LocalDate.now();
        return persistencePort.findByStatus(ChangeStatus.APPROVED).stream()
                .filter(request -> request.isReadyToApply(today))
                .map(request -> applyApprovedChange(request.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataChangeRequest> findPendingRequests() {
        return persistencePort.findByStatus(ChangeStatus.REQUESTED);
    }

    private MasterDataChangeRequest findRequired(Long requestId) {
        return persistencePort.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("ë‰???‚ÂƒìŒ?‚ï???– ??????ë’¿??ˆë–. ID: " + requestId));
    }
}

