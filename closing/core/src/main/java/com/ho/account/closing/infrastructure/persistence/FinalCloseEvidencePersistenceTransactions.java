package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FinalCloseEvidencePersistenceTransactions {

    private final FinalCloseEvidenceSetRepository setRepository;
    private final FinalCloseEvidenceControlRepository controlRepository;
    private final FinalCloseEvidenceTotalRepository totalRepository;

    public FinalCloseEvidencePersistenceTransactions(
            FinalCloseEvidenceSetRepository setRepository,
            FinalCloseEvidenceControlRepository controlRepository,
            FinalCloseEvidenceTotalRepository totalRepository) {
        this.setRepository = setRepository;
        this.controlRepository = controlRepository;
        this.totalRepository = totalRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FinalCloseEvidenceSet insert(FinalCloseEvidenceSet evidenceSet) {
        FinalCloseEvidenceSetEntity persistedSet =
                setRepository.saveAndFlush(new FinalCloseEvidenceSetEntity(evidenceSet));

        List<FinalCloseEvidenceControlEntity> persistedControls = new ArrayList<>();
        for (FinalCloseEvidenceControl control : evidenceSet.controls()) {
            persistedControls.add(new FinalCloseEvidenceControlEntity(persistedSet.getId(), control));
        }
        persistedControls = controlRepository.saveAllAndFlush(persistedControls);

        List<FinalCloseEvidenceTotalEntity> totals = new ArrayList<>();
        for (int controlIndex = 0; controlIndex < persistedControls.size(); controlIndex++) {
            Long controlId = persistedControls.get(controlIndex).getId();
            for (FinalCloseEvidenceTotal total : evidenceSet.controls().get(controlIndex).totals()) {
                totals.add(new FinalCloseEvidenceTotalEntity(controlId, total));
            }
        }
        totalRepository.saveAllAndFlush(totals);
        return reconstruct(persistedSet);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<FinalCloseEvidenceSet> findByEvidenceSetId(String evidenceSetId) {
        return setRepository.findByEvidenceSetId(evidenceSetId).map(this::reconstruct);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<FinalCloseEvidenceSet> findLatestByCalendarId(Long calendarId) {
        return setRepository.findFirstByCalendarIdOrderByObservedAtDescIdDesc(calendarId)
                .map(this::reconstruct);
    }

    private FinalCloseEvidenceSet reconstruct(FinalCloseEvidenceSetEntity set) {
        List<FinalCloseEvidenceControlEntity> controls = controlRepository
                .findByEvidenceSetDbIdOrderByTypeAscSourceSystemAscSourceRunIdAscIdAsc(set.getId());
        if (controls.isEmpty()) {
            return set.toDomain(List.of());
        }

        List<Long> controlIds = controls.stream().map(FinalCloseEvidenceControlEntity::getId).toList();
        Map<Long, List<FinalCloseEvidenceTotal>> totalsByControlId = new HashMap<>();
        for (FinalCloseEvidenceTotalEntity total :
                totalRepository.findByControlIdInOrderByControlIdAscAccountCodeAscCurrencyCodeAscIdAsc(controlIds)) {
            totalsByControlId.computeIfAbsent(total.getControlId(), ignored -> new ArrayList<>())
                    .add(total.toDomain());
        }

        List<FinalCloseEvidenceControl> domainControls = controls.stream()
                .map(control -> control.toDomain(
                        totalsByControlId.getOrDefault(control.getId(), List.of())))
                .toList();
        return set.toDomain(domainControls);
    }
}
