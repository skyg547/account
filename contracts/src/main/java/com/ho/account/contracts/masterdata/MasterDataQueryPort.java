package com.ho.account.contracts.masterdata;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * 여러 거래처 코드를 한 번에 벌크 조회하여 거래처 코드별 BusinessPartnerRef Map을 반환합니다.
     *
     * <p><b>[N+1 쿼리 방지 및 데이터베이스 I/O 성능 최적화]</b><br>
     * 반복문 내에서 단건 조회 메서드를 N번 호출할 경우 N번의 DB Network Round-Trip이 발생하여
     * 심각한 성능 저하(N+1 Problem)가 일어납니다. 이 포트 메서드는 대상 거래처 코드 집합을
     * SQL IN 절 등을 통해 1회의 벌크 쿼리로 일괄 조회하도록 지원합니다.<br>
     * 기본 구현(default method)은 전달받은 코드들을 단건 조회로 Fallback하여 호환성을 유지하며,
     * 모듈 어댑터(MonolithMasterDataQueryAdapter 등)에서 SQL IN 절로 재정의하여 1회 쿼리로 최적화합니다.</p>
     *
     * @param businessPartnerCodes 조회 대상 거래처 코드 컬렉션
     * @return 거래처 코드를 키(Key)로, 거래처 정보(BusinessPartnerRef)를 값(Value)으로 하는 Map
     */
    default Map<String, BusinessPartnerRef> findAllByPartnerCodes(Collection<String> businessPartnerCodes) {
        if (businessPartnerCodes == null || businessPartnerCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return businessPartnerCodes.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(this::findBusinessPartner)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(BusinessPartnerRef::code, Function.identity(), (a, b) -> a));
    }

    /**
     * 주어진 코드에 해당하는 현재 활성 상태의 부서를 조회합니다.
     */
    Optional<DepartmentRef> findDepartment(String departmentCode);

    /**
     * 특정 일자에 유효한 계정과목을 조회합니다.
     *
     * <p>모든 원격/로컬 제공자가 실제 SCD2 기준일 조회를 구현한 뒤 이 호환 기본 구현을 제거해,
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
