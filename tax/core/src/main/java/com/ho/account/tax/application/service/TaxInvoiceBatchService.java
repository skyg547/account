package com.ho.account.tax.application.service;

import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.tax.application.port.in.TaxInvoiceBatchUseCase;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세금계산서 일괄 배치 검증을 담당하는 애플리케이션 서비스입니다.
 *
 * <p>헥사고날 아키텍처의 Inbound Port({@link TaxInvoiceBatchUseCase})를 구현하며,
 * 아웃바운드 포트({@link MasterDataQueryPort}, {@link TaxInvoicePersistencePort})를 통해
 * 데이터베이스 및 외부 기준정보 모듈과 통신합니다.</p>
 */
@Service
@Transactional(readOnly = true)
public class TaxInvoiceBatchService implements TaxInvoiceBatchUseCase {

    private final TaxInvoicePersistencePort taxInvoicePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;

    public TaxInvoiceBatchService(
            TaxInvoicePersistencePort taxInvoicePersistencePort,
            MasterDataQueryPort masterDataQueryPort) {
        this.taxInvoicePersistencePort = taxInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
    }

    /**
     * 지정된 기간 동안의 매입 세금계산서들을 일괄 검증합니다.
     *
     * <p><b>[N+1 쿼리 문제 및 벌크 쿼리 최적화 설계 원칙 (Pedagogical Comments)]</b><br>
     * 1. <b>N+1 쿼리 문제 (N+1 Problem)</b>:<br>
     *    기존 방식에서는 세금계산서 N건을 순회하는 반복문(for-loop) 내부에서 거래처 단건 조회 메서드
     *    ({@code masterDataQueryPort.findBusinessPartner(...)})를 N번 반복 호출했습니다.<br>
     *    이 구조는 N번의 추가 DB Network Round-Trip 오버헤드가 발생하여 대량 배치 처리 시 I/O 병목 및 성능 저하를 일으킵니다.<br>
     * 2. <b>벌크 쿼리 패턴 (Bulk Query Pattern) 적용 방식</b>:<br>
     *    - <b>Step 1 (식별자 수집 &amp; Set 중복 제거)</b>: 검증 대상 매입 세금계산서들로부터 거래처 코드 목록을 추출하고
     *      Set 컬렉션으로 수집하여 동일 거래처 코드 중복 조회를 방지합니다.<br>
     *    - <b>Step 2 (벌크 쿼리 일괄 조회)</b>: 추출된 거래처 코드 Set으로 {@code masterDataQueryPort.findAllByPartnerCodes(...)}를
     *      1회 호출합니다. 하위 어댑터에서는 SQL {@code IN} 절을 이용하여 단 1회의 쿼리로 모든 거래처를 일괄 조회합니다.<br>
     *    - <b>Step 3 (로컬 Map 캐싱 및 O(1) 조율)</b>: 조회된 결과를 거래처 코드를 Key로 하는 {@code Map<String, BusinessPartnerRef>}에
     *      구성하여, 반복문 내부에서는 {@code Map.containsKey()} 또는 {@code Map.get()}으로 시간 복잡도 O(1)에 바로 검증합니다.</p>
     *
     * @param startDate 검증 시작일
     * @param endDate 검증 종료일
     * @return 검증 대상 건수 및 성공 건수를 포함한 {@link TaxInvoiceValidationResult}
     */
    @Override
    public TaxInvoiceValidationResult validatePurchaseInvoices(LocalDate startDate, LocalDate endDate) {
        List<TaxInvoice> invoices = taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate);

        // Step 1: 매입 세금계산서의 거래처 코드(businessPartnerCode) 집합 수집 (Set으로 중복 제거)
        Set<String> purchasePartnerCodes = invoices.stream()
                .filter(TaxInvoice::isPurchaseType)
                .map(TaxInvoice::getBusinessPartnerCode)
                .collect(Collectors.toSet());

        // Step 2: 벌크 쿼리(findAllByPartnerCodes)로 거래처 정보 1회 일괄 조회 (N+1 쿼리 및 DB I/O 최적화)
        Map<String, BusinessPartnerRef> partnerMap = masterDataQueryPort.findAllByPartnerCodes(purchasePartnerCodes);

        // Step 3: 반복문 순회 시 로컬 Map.containsKey()로 시간 복잡도 O(1) 시간 내 검증 수행
        int validated = 0;
        for (TaxInvoice invoice : invoices) {
            if (!invoice.isPurchaseType()) {
                continue;
            }
            invoice.validateAmounts();
            if (!partnerMap.containsKey(invoice.getBusinessPartnerCode())) {
                throw new IllegalStateException(
                        "Business partner missing for tax invoice " + invoice.getIssueId());
            }
            validated++;
        }
        return new TaxInvoiceValidationResult(invoices.size(), validated);
    }
}
