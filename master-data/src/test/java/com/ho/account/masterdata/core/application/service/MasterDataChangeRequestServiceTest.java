package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDataChangeRequestServiceTest {

    private final InMemoryPort port = new InMemoryPort();
    private final RecordingApplier applier = new RecordingApplier();
    private final MasterDataChangeRequestService service = new MasterDataChangeRequestService(port, List.of(applier));

    @Test
    void requestsAndApprovesMasterDataChange() {
        MasterDataChangeRequest requested = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.BUSINESS_PARTNER,
                "BP-001",
                ChangeType.CREATE,
                LocalDate.now().plusDays(3),
                1,
                "operator",
                "Create vendor partner",
                "{\"businessPartnerName\":\"Acme Vendor\"}"));

        MasterDataChangeRequest approved = service.approve(requested.getId(), "manager");

        assertThat(approved.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo("manager");
    }

    @Test
    void findsOnlyPendingRequests() {
        MasterDataChangeRequest first = service.requestChange(command("D-001"));
        MasterDataChangeRequest second = service.requestChange(command("D-002"));
        service.approve(second.getId(), "manager");

        assertThat(service.findPendingRequests())
                .extracting(MasterDataChangeRequest::getId)
                .containsExactly(first.getId());
    }

    @Test
    void appliesApprovedChangeAndMarksApplied() {
        MasterDataChangeRequest requested = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT,
                "D-APPLY",
                ChangeType.UPDATE,
                LocalDate.now(),
                1,
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
        MasterDataChangeRequest due = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT, "D-DUE", ChangeType.UPDATE, LocalDate.now(), 1,
                "operator", "Due change", "{}"));
        MasterDataChangeRequest future = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.DEPARTMENT, "D-FUTURE", ChangeType.UPDATE, LocalDate.now().plusDays(1), 1,
                "operator", "Future change", "{}"));
        service.approve(due.getId(), "manager");
        service.approve(future.getId(), "manager");

        assertThat(service.applyDueApprovedChanges())
                .extracting(MasterDataChangeRequest::getId)
                .containsExactly(due.getId());
        assertThat(applier.appliedRequests).containsExactly(due.getId());
    }

    @Test
    void applyApprovedChangeFailsClosedWhenNoApplierSupportsTargetType() {
        MasterDataChangeRequestService unsupportedService = new MasterDataChangeRequestService(port, List.of());
        MasterDataChangeRequest requested = service.requestChange(new MasterDataChangeRequestCommand(
                MasterDataType.CURRENCY,
                "KRW",
                ChangeType.UPDATE,
                LocalDate.now(),
                1,
                "operator",
                "Unsupported type",
                "{}"));
        service.approve(requested.getId(), "manager");

        assertThatThrownBy(() -> unsupportedService.applyApprovedChange(requested.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No MasterDataChangeApplier supports targetType: CURRENCY");
        assertThat(requested.getStatus()).isEqualTo(ChangeStatus.APPROVED);
    }

    private MasterDataChangeRequestCommand command(String key) {
        return new MasterDataChangeRequestCommand(MasterDataType.DEPARTMENT, key, ChangeType.UPDATE,
                LocalDate.now().plusDays(1), 1, "operator", "Routine department change", "{}");
    }

    private static final class InMemoryPort implements MasterDataChangeRequestPersistencePort {
        private final List<MasterDataChangeRequest> store = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Optional<MasterDataChangeRequest> findById(Long id) {
            return store.stream().filter(request -> request.getId().equals(id)).findFirst();
        }

        @Override
        public List<MasterDataChangeRequest> findAll() {
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
        public boolean supports(MasterDataType targetType) {
            return targetType == MasterDataType.DEPARTMENT;
        }

        @Override
        public void apply(MasterDataChangeRequest request) {
            appliedRequests.add(request.getId());
        }
    }
}

