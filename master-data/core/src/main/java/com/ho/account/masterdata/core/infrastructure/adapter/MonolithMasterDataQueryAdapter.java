package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 같은 프로세스에서 contracts 조회 포트를 master-data 도메인 포트에 연결하는 어댑터입니다.
 *
 * <p>현재 조회와 기준일 조회를 분리해 Closing처럼 과거 시점이 중요한 업무가 오늘의 기준정보를
 * 잘못 사용하는 것을 막습니다. JPA repository를 직접 호출하지 않고 도메인 출력 포트를
 * 거치므로, contracts 응답을 만드는 코드까지 JPA 전용 엔티티가 새지 않습니다.</p>
 */
@Component
public class MonolithMasterDataQueryAdapter implements MasterDataQueryPort {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;

    public MonolithMasterDataQueryAdapter(
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            BusinessPartnerPersistencePort businessPartnerPersistencePort,
            DepartmentPersistencePort departmentPersistencePort) {
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
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
        return accountSubjectPersistencePort.findByCodeAt(accountCode, effectiveDate)
                .map(this::toAccountSubjectRef);
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode)
                .map(this::toBusinessPartnerRef);
    }

    @Override
    public Map<String, BusinessPartnerRef> findAllByPartnerCodes(Collection<String> businessPartnerCodes) {
        if (businessPartnerCodes == null || businessPartnerCodes.isEmpty()) {
            return Map.of();
        }
        return businessPartnerPersistencePort.findAllByBusinessPartnerCodeIn(businessPartnerCodes).stream()
                .map(this::toBusinessPartnerRef)
                .collect(Collectors.toMap(BusinessPartnerRef::code, Function.identity(), (a, b) -> a));
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartnerAt(
            String businessPartnerCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
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
        return departmentPersistencePort.findActiveByCodeAt(departmentCode, effectiveDate)
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

