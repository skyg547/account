package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 같은 프로세스에서 contracts 조회 포트를 master-data JPA 저장소에 연결하는 어댑터입니다.
 *
 * <p>현재 조회와 기준일 조회를 분리해 Closing처럼 과거 시점이 중요한 업무가 오늘의 기준정보를
 * 잘못 사용하는 것을 막습니다. 거래처는 JPA repository를 직접 호출하지 않고 도메인 출력 포트를
 * 거치므로, contracts 응답을 만드는 코드까지 JPA 전용 엔티티가 새지 않습니다.</p>
 */
@Component
public class MonolithMasterDataQueryAdapter implements MasterDataQueryPort {

    private final AccountSubjectRepository accountSubjectRepository;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final DepartmentRepository departmentRepository;

    public MonolithMasterDataQueryAdapter(
            AccountSubjectRepository accountSubjectRepository,
            BusinessPartnerPersistencePort businessPartnerPersistencePort,
            DepartmentRepository departmentRepository) {
        this.accountSubjectRepository = accountSubjectRepository;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
        return findAccountSubjectAt(accountCode, LocalDate.now());
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubjectAt(
            String accountCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return accountSubjectRepository.findActiveByCode(accountCode, effectiveDate)
                .map(this::toAccountSubjectRef);
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode)
                .map(this::toBusinessPartnerRef);
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartnerAt(
            String businessPartnerCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        // @todo BusinessPartner.terminate가 과거 행의 legacy useYn도 false로 바꾸므로 Ref.active는
        // 기준일 당시의 활성 상태를 완전히 재현하지 못합니다. 버전 종료와 업무 비활성 상태를 분리하고
        // 기존 행을 이관한 뒤 UPDATE/DEACTIVATE 과거 조회 통합 테스트를 통과시키는 것이 완료 조건입니다.
        // 기준일 조건은 출력 포트의 계약으로 전달하고, 실제 JPQL/엔티티 변환은 persistence가 맡습니다.
        return businessPartnerPersistencePort
                .findEffectiveByBusinessPartnerCode(businessPartnerCode, effectiveDate)
                .map(this::toBusinessPartnerRef);
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String departmentCode) {
        return findDepartmentAt(departmentCode, LocalDate.now());
    }

    @Override
    public Optional<DepartmentRef> findDepartmentAt(
            String departmentCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return departmentRepository.findActiveByCode(departmentCode, effectiveDate)
                .map(this::toDepartmentRef);
    }

    private AccountSubjectRef toAccountSubjectRef(AccountSubject account) {
        return new AccountSubjectRef(
                account.getCode(),
                account.getName(),
                account.isUnsettled(),
                account.isFixedAsset(),
                account.getBalanceType() != null ? account.getBalanceType().name() : null,
                account.getCategory() != null ? account.getCategory().name() : null);
    }

    private BusinessPartnerRef toBusinessPartnerRef(BusinessPartner partner) {
        return new BusinessPartnerRef(
                partner.getBusinessPartnerCode(),
                partner.getBusinessPartnerName(),
                partner.getPartnerType().name(),
                partner.getUseYn());
    }

    private DepartmentRef toDepartmentRef(Department department) {
        return new DepartmentRef(
                department.getCode(),
                department.getName(),
                department.getType() != null ? department.getType().name() : null);
    }
}
