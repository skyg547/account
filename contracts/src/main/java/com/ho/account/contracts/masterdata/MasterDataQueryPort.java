package com.ho.account.contracts.masterdata;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * 타 모듈이 기준정보 엔티티를 직접 참조하지 않고 Ref 형태로 조회하기 위한 통합 계약입니다.
 *
 * <p>호출 모듈에서는 아웃바운드 포트로 사용하고, master-data 모듈에서는 어댑터가 구현합니다.
 * REST/Feign/JPA 같은 연결 기술은 이 계약 밖의 어댑터가 결정합니다.</p>
 */
public interface MasterDataQueryPort {

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 계정과목을 조회합니다.
     */
    Optional<AccountSubjectRef> findAccountSubject(String accountCode);

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 거래처를 조회합니다.
     */
    Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode);

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 부서를 조회합니다.
     */
    Optional<DepartmentRef> findDepartment(String departmentCode);

    /**
     * 특정 일자에 유효한 계정과목을 조회합니다.
     *
     * <p>@todo 모든 원격/로컬 제공자가 실제 SCD2 기준일 조회를 구현한 뒤 이 호환 기본 구현을 제거해,
     * 기준일을 무시하는 어댑터가 컴파일 단계에서 발견되게 한다.</p>
     */
    default Optional<AccountSubjectRef> findAccountSubjectAt(String accountCode, LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return findAccountSubject(accountCode);
    }

    /**
     * 특정 일자에 유효한 거래처를 조회합니다.
     */
    default Optional<BusinessPartnerRef> findBusinessPartnerAt(
            String businessPartnerCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return findBusinessPartner(businessPartnerCode);
    }

    /**
     * 특정 일자에 유효한 부서를 조회합니다.
     */
    default Optional<DepartmentRef> findDepartmentAt(String departmentCode, LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return findDepartment(departmentCode);
    }
}