package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDataChangeRequestServiceTest {

    private final InMemoryPort port = new InMemoryPort();
    private final InMemoryVersionQueryPort versionQueryPort = new InMemoryVersionQueryPort();
    private final RecordingApplier applier = new RecordingApplier();
    private final MasterDataChangeRequestService service =
            new MasterDataChangeRequestService(port, versionQueryPort, List.of(applier));

    @Test
    void requestsAndApprovesFirstMasterDataVersion() {
        MasterDataChangeRequest requested = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT,
                "D-NEW",
                ChangeType.CREATE,
                LocalDate.now().plusDays(3),
                1,
                "operator",
                "Create department",
                """
                {"name":"New Department"}
                """));

        MasterDataChangeRequest approved = service.approve(requested.getId(), "manager");

        assertThat(approved.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo("manager");
    }

    @Test
    void findsOnlyPendingRequests() {
        prepareExistingVersion("D-001", 1);
        prepareExistingVersion("D-002", 1);
        MasterDataChangeRequest first = service.requestChange(updateCommand("D-001", 2));
        MasterDataChangeRequest second = service.requestChange(updateCommand("D-002", 2));
        service.approve(second.getId(), "manager");

        assertThat(service.findPendingRequests())
                .extracting(MasterDataChangeRequest::getId)
                .containsExactly(first.getId());
    }

    @Test
    void appliesApprovedChangeAndMarksApplied() {
        prepareExistingVersion("D-APPLY", 1);
        MasterDataChangeRequest requested = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT,
                "D-APPLY",
                ChangeType.UPDATE,
                LocalDate.now(),
                2,
                "operator",
                "Update department name",
                "{}"));
        service.approve(requested.getId(), "manager");

        MasterDataChangeRequest applied = service.applyApprovedChange(requested.getId());

        assertThat(applied.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(applier.appliedRequests).containsExactly(requested.getId());
    }

    @Test
    void appliesOnlyApprovedChangesWhoseEffectiveDateHasArrived() {
        prepareExistingVersion("D-DUE", 1);
        prepareExistingVersion("D-FUTURE", 1);
        MasterDataChangeRequest due = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT, "D-DUE", ChangeType.UPDATE, LocalDate.now(), 2,
                "operator", "Due change", "{}"));
        MasterDataChangeRequest future = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT, "D-FUTURE", ChangeType.UPDATE, LocalDate.now().plusDays(1), 2,
                "operator", "Future change", "{}"));
        service.approve(due.getId(), "manager");
        service.approve(future.getId(), "manager");

        assertThat(service.applyDueApprovedChanges())
                .extracting(MasterDataChangeRequest::getId)
                .containsExactly(due.getId());
        assertThat(applier.appliedRequests).containsExactly(due.getId());
    }

    @Test
    void requestFailsClosedWhenNoApplierSupportsTargetType() {
        MasterDataChangeRequestService unsupportedService =
                new MasterDataChangeRequestService(port, versionQueryPort, List.of());

        assertThatThrownBy(() -> unsupportedService.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.CURRENCY,
                "KRW",
                ChangeType.CREATE,
                LocalDate.now(),
                1,
                "operator",
                "Unsupported type",
                "{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No MasterDataChangeApplier supports targetType: CURRENCY");

        assertThat(port.allRequests()).isEmpty();
    }

    @Test
    void rejectsRequestWhoseVersionDoesNotFollowPersistedScd2History() {
        prepareExistingVersion("D-STALE", 2);

        assertThatThrownBy(() -> service.requestChange(updateCommand("D-STALE", 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected=3")
                .hasMessageContaining("requested=2");

        assertThat(port.allRequests()).isEmpty();
    }

    @Test
    void approvalRechecksVersionAfterAnotherChangeWasApplied() {
        prepareExistingVersion("D-CONFLICT", 1);
        MasterDataChangeRequest requested = service.requestChange(updateCommand("D-CONFLICT", 2));
        prepareExistingVersion("D-CONFLICT", 2);

        assertThatThrownBy(() -> service.approve(requested.getId(), "manager"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected=3");
        assertThat(requested.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
    }

    @Test
    void duplicateApplierOwnershipFailsAtServiceConstruction() {
        assertThatThrownBy(() -> new MasterDataChangeRequestService(
                port,
                versionQueryPort,
                List.of(new RecordingApplier(), new RecordingApplier())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple MasterDataChangeAppliers")
                .hasMessageContaining("DEPARTMENT");
    }

    @Test
    void sequentialRetryWithSameSourceReferenceReturnsExistingRequest() {
        MasterDataChangeRequestCommand command = createCommand(
                "D-IDEMPOTENT", "governance-approval-id=55");

        MasterDataChangeRequest first = service.requestChange(command);
        MasterDataChangeRequest retried = service.requestChange(command);

        assertThat(retried).isSameAs(first);
        assertThat(port.allRequests()).containsExactly(first);
    }

    @Test
    void sameSourceReferenceCannotBeReusedForDifferentChange() {
        service.requestChange(createCommand("D-FIRST", "governance-approval-id=55"));

        assertThatThrownBy(() -> service.requestChange(
                createCommand("D-OTHER", "governance-approval-id=55")))
                .isInstanceOf(MasterDataIdempotencyConflictException.class)
                .hasMessageContaining("governance-approval-id=55");
        assertThat(port.allRequests()).hasSize(1);
    }

    private MasterDataChangeRequestCommand updateCommand(String key, int requestedVersion) {
        return new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT,
                key,
                ChangeType.UPDATE,
                LocalDate.now().plusDays(1),
                requestedVersion,
                "operator",
                "Routine department change",
                "{}");
    }

    private MasterDataChangeRequestCommand createCommand(String key, String sourceReference) {
        return new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT,
                key,
                ChangeType.CREATE,
                LocalDate.now().plusDays(1),
                1,
                "operator",
                "Create department",
                "{}",
                sourceReference);
    }

    private void prepareExistingVersion(String key, long versionCount) {
        versionQueryPort.put(MasterDataType.DEPARTMENT, key, versionCount);
    }

    private static final class InMemoryVersionQueryPort implements MasterDataVersionQueryPort {
        private final Map<String, Long> versions = new HashMap<>();

        @Override
        public long countPersistedVersions(MasterDataType targetType, String targetKey) {
            return versions.getOrDefault(key(targetType, targetKey), 0L);
        }

        private void put(MasterDataType targetType, String targetKey, long versionCount) {
            versions.put(key(targetType, targetKey), versionCount);
        }

        private static String key(MasterDataType targetType, String targetKey) {
            return targetType + ":" + targetKey;
        }
    }

    private static final class InMemoryPort implements MasterDataChangeRequestPersistencePort {
        private final List<MasterDataChangeRequest> store = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Optional<MasterDataChangeRequest> findById(Long id) {
            return store.stream().filter(request -> request.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<MasterDataChangeRequest> findByIdForUpdate(Long id) {
            return findById(id);
        }

        @Override
        public Optional<MasterDataChangeRequest> findBySourceReference(String sourceReference) {
            return store.stream()
                    .filter(request -> sourceReference.equals(request.getSourceReference()))
                    .findFirst();
        }

        private List<MasterDataChangeRequest> allRequests() {
            return new ArrayList<>(store);
        }

        @Override
        public List<MasterDataChangeRequest> findByStatus(ChangeStatus status) {
            return store.stream().filter(request -> request.getStatus() == status).toList();
        }

        @Override
        public List<MasterDataChangeRequest> findReadyToApply(LocalDate effectiveDate, int limit) {
            return store.stream()
                    .filter(request -> request.getStatus() == ChangeStatus.APPROVED)
                    .filter(request -> !request.getEffectiveDate().isAfter(effectiveDate))
                    .sorted(Comparator.comparing(MasterDataChangeRequest::getEffectiveDate)
                            .thenComparing(MasterDataChangeRequest::getRequestedAt))
                    .limit(limit)
                    .toList();
        }

        @Override
        public MasterDataChangeRequest save(MasterDataChangeRequest request) {
            if (request.getId() == null) {
                setId(request, sequence++);
                store.add(request);
            }
            return request;
        }

        private void setId(MasterDataChangeRequest request, Long id) {
            try {
                java.lang.reflect.Field field = MasterDataChangeRequest.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(request, id);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final class RecordingApplier implements MasterDataChangeApplier {
        private final List<Long> appliedRequests = new ArrayList<>();

        @Override
        public MasterDataType targetType() {
            return MasterDataType.DEPARTMENT;
        }

        @Override
        public void apply(MasterDataChangeRequest request) {
            appliedRequests.add(request.getId());
        }
    }
}
