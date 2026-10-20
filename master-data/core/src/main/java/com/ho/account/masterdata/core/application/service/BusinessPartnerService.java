package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.application.query.BusinessPartnerSummary;
import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 거래처(Business Partner) 마스터 데이터를 관리하는 application service입니다.
 *
 * <p>서비스는 트랜잭션 경계를 열고 도메인과 출력 포트를 조정합니다. 유효기간 계산과 코드
 * 불변성은 {@link BusinessPartner}가 책임지며, 서비스는 검증된 현재 버전 종료와 새 버전 저장이
 * 한 트랜잭션에서 함께 성공하거나 함께 롤백되도록 보장합니다.</p>
 */
@Service
@Transactional
public class BusinessPartnerService implements BusinessPartnerUseCase {

    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataBusinessKeyLockPort businessKeyLockPort;
    private final MasterDataAuditPort auditPort;

    BusinessPartnerService(BusinessPartnerPersistencePort businessPartnerPersistencePort,
            MasterDataBusinessKeyLockPort businessKeyLockPort) {
        this(businessPartnerPersistencePort, businessKeyLockPort, event -> { });
    }

    @Autowired
    public BusinessPartnerService(BusinessPartnerPersistencePort businessPartnerPersistencePort,
            MasterDataBusinessKeyLockPort businessKeyLockPort, MasterDataAuditPort auditPort) {
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.businessKeyLockPort = businessKeyLockPort;
        this.auditPort = auditPort;
    }

    /**
     * 신규 거래처를 등록합니다.
     */
    @Override
    public BusinessPartner createBusinessPartner(BusinessPartnerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("거래처 등록 명령은 필수입니다.");
        }
        BusinessPartner businessPartner = command.toDomain();
        // 도메인이 정규화한 실제 저장 코드로 잠가 공백이 있는 직접 입력도 같은 키로 직렬화합니다.
        businessKeyLockPort.lock(MasterDataType.BUSINESS_PARTNER, businessPartner.getBusinessPartnerCode());
        // 조회 포트는 날짜/useYn과 무관한 전체 이력 존재 여부를 확인합니다.
        if (businessPartnerPersistencePort.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new MasterDataVersionConflictException("이미 이력이 존재하는 거래처 코드입니다: "
                    + businessPartner.getBusinessPartnerCode());
        }
        businessPartner.setAuditUser(MasterDataActorContext.auditUser());
        BusinessPartner saved = businessPartnerPersistencePort.save(businessPartner);
        MasterDataAuditRecorder.direct(auditPort, "DIRECT_CREATE", MasterDataType.BUSINESS_PARTNER,
                saved.getBusinessPartnerCode(), null, saved.getId());
        return saved;
    }

    /**
     * 모든 거래처를 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public PageSlice<BusinessPartnerSummary> getAllBusinessPartners(PageWindow page) {
        return businessPartnerPersistencePort.findAllSummaries(page);
    }

    /** 오늘을 기준으로 업무 사용 상태와 SCD2 유효기간을 함께 적용합니다. */
    @Override
    @Transactional(readOnly = true)
    public PageSlice<BusinessPartnerSummary> getActiveBusinessPartners(PageWindow page) {
        return businessPartnerPersistencePort.findActiveSummaries(LocalDate.now(), page);
    }

    /**
     * 거래처 코드로 단건 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode);
    }

    /**
     * 거래처 이름으로 검색합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public PageSlice<BusinessPartnerSummary> searchBusinessPartnersByName(String name, PageWindow page) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("거래처 검색어는 필수입니다.");
        }
        return businessPartnerPersistencePort.searchActiveByName(name.trim(), LocalDate.now(), page);
    }

    /**
     * 거래처 정보를 수정합니다. (SCD2 원칙에 따라 기존 버전을 종료하고 새 버전을 생성합니다)
     */
    @Override
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("거래처 수정 명령은 필수입니다.");
        }
        lockBusinessKeyById(id);
        BusinessPartner currentActive = businessPartnerPersistencePort.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));
        command.requireUnchangedScreening(currentActive);

        // 도메인이 새 버전을 먼저 완전히 검증한 뒤에만 현재 버전을 닫습니다.
        // 이 순서 덕분에 잘못된 날짜나 코드 변경 요청이 현재 정상 버전을 먼저 종료하지 않습니다.
        LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : LocalDate.now();
        LocalDate newValidTo = command.validTo() != null
                ? command.validTo()
                : BusinessPartner.OPEN_ENDED_VALID_TO;
        BusinessPartner newVersion = currentActive.createNextVersion(
                command.businessPartnerCode(),
                command.businessPartnerName(),
                command.registrationNumber(),
                command.ceoName(),
                command.businessType(),
                command.businessItem(),
                command.partnerType(),
                command.useYn(),
                command.kycStatus(),
                command.riskRating(),
                newValidFrom,
                newValidTo);
        newVersion.setAuditUser(MasterDataActorContext.auditUser());

        // 두 save는 클래스의 @Transactional 경계 안에 있으므로 하나만 저장되는 부분 성공을 막습니다.
        currentActive.closeVersion(newValidFrom.minusDays(1));
        businessPartnerPersistencePort.save(currentActive);
        BusinessPartner saved = businessPartnerPersistencePort.save(newVersion);
        MasterDataAuditRecorder.direct(auditPort, "DIRECT_UPDATE", MasterDataType.BUSINESS_PARTNER,
                saved.getBusinessPartnerCode(), currentActive.getId(), saved.getId());
        return saved;
    }

    /**
     * 거래처를 논리적으로 삭제(비활성화)합니다.
     */
    @Override
    public void deleteBusinessPartner(Long id) {
        deleteBusinessPartner(id, LocalDate.now());
    }

    @Override
    public void deleteBusinessPartner(Long id, LocalDate effectiveDate) {
        lockBusinessKeyById(id);
        BusinessPartner businessPartner = businessPartnerPersistencePort.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        // aggregate의 같은 날짜 no-op을 승인 반영 성공으로 오인하지 않도록 잠금 뒤 확인합니다.
        LocalDate terminationDate = MasterDataValidityPolicy.requireNewTerminationDate(
                effectiveDate, businessPartner.getValidFrom(), businessPartner.getValidTo());
        businessPartner.terminate(terminationDate);
        businessPartner.setAuditUser(MasterDataActorContext.auditUser());
        businessPartnerPersistencePort.save(businessPartner);
        MasterDataAuditRecorder.direct(auditPort, "DIRECT_DEACTIVATE", MasterDataType.BUSINESS_PARTNER,
                businessPartner.getBusinessPartnerCode(), businessPartner.getId(), businessPartner.getId());
    }

    private void lockBusinessKeyById(Long id) {
        // 먼저 aggregate를 읽으면 잠금 대기 후에도 영속성 캐시에 이전 유효기간이 남을 수 있습니다.
        String businessKey = businessPartnerPersistencePort.findBusinessKeyById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));
        businessKeyLockPort.lock(MasterDataType.BUSINESS_PARTNER, businessKey);
    }
}
