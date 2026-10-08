package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import org.springframework.stereotype.Service;
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

    public BusinessPartnerService(BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
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
        // 조회 포트는 날짜/useYn과 무관한 전체 이력 존재 여부를 확인합니다.
        if (businessPartnerPersistencePort.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new MasterDataVersionConflictException("이미 이력이 존재하는 거래처 코드입니다: "
                    + businessPartner.getBusinessPartnerCode());
        }
        return businessPartnerPersistencePort.save(businessPartner);
    }

    /**
     * 모든 거래처를 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerPersistencePort.findAll();
    }

    /**
     * 활성화된 거래처만 조회합니다. (Legacy UseYn 플래그 기반)
     */
    @Override
    @Transactional(readOnly = true)
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerPersistencePort.findByUseYnTrue();
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
    public List<BusinessPartner> searchBusinessPartnersByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("거래처 검색어는 필수입니다.");
        }
        return businessPartnerPersistencePort.searchActiveByName(name.trim(), LocalDate.now());
    }

    /**
     * 거래처 정보를 수정합니다. (SCD2 원칙에 따라 기존 버전을 종료하고 새 버전을 생성합니다)
     */
    @Override
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("거래처 수정 명령은 필수입니다.");
        }
        BusinessPartner currentActive = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

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

        // 두 save는 클래스의 @Transactional 경계 안에 있으므로 하나만 저장되는 부분 성공을 막습니다.
        currentActive.closeVersion(newValidFrom.minusDays(1));
        businessPartnerPersistencePort.save(currentActive);
        return businessPartnerPersistencePort.save(newVersion);
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
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        // 종료일 범위와 중복 종료는 aggregate가 검증하므로 모든 인바운드 경로에 같은 규칙이 적용됩니다.
        businessPartner.terminate(effectiveDate);
        businessPartnerPersistencePort.save(businessPartner);
    }
}
