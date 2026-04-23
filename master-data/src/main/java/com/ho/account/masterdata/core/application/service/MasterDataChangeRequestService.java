package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.usecase.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.port.out.MasterDataChangeRequestPersistencePort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 留덉뒪??蹂寃쎄?由??좎뒪耳?댁뒪 ?쒕퉬?ㅼ엯?덈떎.
 *
 * <p>???쒕퉬?ㅻ뒗 蹂寃쎌슂泥?쓽 ?곹깭 ?먮쫫留?愿由ы빀?덈떎. ?ㅼ젣 怨꾩젙怨쇰ぉ/嫄곕옒泥?row瑜?諛붽씀???묒뾽?
 * ?뱀씤???붿껌???곸슜?섎뒗 蹂꾨룄 application service ?먮뒗 batch媛 ?대떦?섎룄濡?遺꾨━?⑸땲??</p>
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
            throw new IllegalStateException("?뱀씤 ?곹깭?닿퀬 ?곸슜?쇱씠 ?꾨옒???붿껌留??곸슜?????덉뒿?덈떎.");
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
                .orElseThrow(() -> new IllegalArgumentException("留덉뒪??蹂寃쎌슂泥?쓣 李얠쓣 ???놁뒿?덈떎. ID: " + requestId));
    }
}
