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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    /**
     * 배치 검증 실행 시 기본으로 사용할 페이징 분할 크기(청크 사이즈)입니다.
     * 메모리 사용량을 예측 가능하고 안정적인 범위 내로 상한 제어하기 위한 기준값입니다.
     */
    private static final int DEFAULT_PAGE_SIZE = 500;

    private final TaxInvoicePersistencePort taxInvoicePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;

    public TaxInvoiceBatchService(
            TaxInvoicePersistencePort taxInvoicePersistencePort,
            MasterDataQueryPort masterDataQueryPort) {
        this.taxInvoicePersistencePort = taxInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
    }

    /**
     * 기본 청크 크기(500건)를 이용하여 지정된 기간 동안의 매입 세금계산서들을 일괄 검증합니다.
     *
     * @param startDate 검증 시작일
     * @param endDate 검증 종료일
     * @return 검증 대상 건수 및 성공 건수를 포함한 {@link TaxInvoiceValidationResult}
     */
    @Override
    public TaxInvoiceValidationResult validatePurchaseInvoices(LocalDate startDate, LocalDate endDate) {
        return validatePurchaseInvoices(startDate, endDate, DEFAULT_PAGE_SIZE);
    }

    /**
     * 지정된 청크 크기(pageSize)로 페이징 분할하여 기간 내 매입 세금계산서들을 일괄 검증합니다.
     *
     * <p><b>[대용량 배치 페이징(Paging) 및 메모리 풋프린트 관리 설계 원칙 (Pedagogical Comments)]</b><br>
     * 1. <b>Heap Out-Of-Memory(OOM) 방지 및 메모리 풋프린트(Memory Footprint) 상한 고정</b>:<br>
     *    기존 방식처럼 전체 세금계산서 데이터를 단일 {@code List}로 메모리에 한 번에 적재하면,
     *    데이터가 수만~수십만 건 이상일 때 힙 메모리 사용량이 선형적으로 증가하여 $O(N)$ 메모리 복잡도를 가지게 됩니다.<br>
     *    본 리팩토링에서는 {@link Pageable} 및 분할 페이지 조회({@code pageSize})를 도입하여,
     *    힙 메모리에 동시에 상주하는 세금계산서 객체 수를 최대 {@code pageSize} (예: 500건) 이내로 엄격히 제한합니다 ($O(pageSize)$).<br>
     *    각 청크(Chunk) 처리 후에는 사용 완료된 객체 참조가 해제되어 JVM Garbage Collector(GC)가 빠르게 Young Generation 영역에서
     *    힙 메모리를 회수할 수 있도록 합니다.<br>
     * 2. <b>N+1 쿼리 방지 및 Bulk Query와의 시너지</b>:<br>
     *    페이지 단위로 조회된 매입 세금계산서 청크 목록에서 거래처 코드 집합({@code Set<String>})을 추출한 후,
     *    {@code masterDataQueryPort.findAllByPartnerCodes(...)} 벌크 쿼리를 1회 호출합니다.<br>
     *    이 방식은 대용량 Paging 분할 조회로 OOM을 예방하는 동시에, 청크 내 N+1 쿼리 오버헤드를 $O(1)$의 벌크 검색 맵 캐싱으로 극복하여
     *    DB Network Round-Trip을 최소화합니다.<br>
     * 3. <b>금융 배치 아키텍처적 확장성</b>:<br>
     *    데이터 규모가 지속적으로 증가하더라도 하드웨어 메모리 증설 없이도 동일한 힙 메모리 사용량 수준에서 안전하게 배치를 실행할 수 있습니다.</p>
     *
     * @param startDate 검증 시작일
     * @param endDate 검증 종료일
     * @param pageSize 페이징 분할 크기 (청크 사이즈)
     * @return 검증 대상 건수 및 성공 건수를 포함한 {@link TaxInvoiceValidationResult}
     */
    @Override
    public TaxInvoiceValidationResult validatePurchaseInvoices(LocalDate startDate, LocalDate endDate, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize는 1 이상이어야 합니다: " + pageSize);
        }

        int scannedCount = 0;
        int validatedCount = 0;
        int pageNumber = 0;
        Page<TaxInvoice> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            page = taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate, pageable);
            List<TaxInvoice> chunk = page.getContent();

            if (chunk.isEmpty()) {
                break;
            }

            // Step 1: 현재 청크 내 매입 세금계산서의 거래처 코드 집합 수집 (Set으로 중복 제거)
            Set<String> purchasePartnerCodes = chunk.stream()
                    .filter(TaxInvoice::isPurchaseType)
                    .map(TaxInvoice::getBusinessPartnerCode)
                    .collect(Collectors.toSet());

            // Step 2: 현재 청크의 거래처 정보 1회 일괄 조회 (N+1 쿼리 및 DB I/O 최적화)
            Map<String, BusinessPartnerRef> partnerMap = purchasePartnerCodes.isEmpty()
                    ? Map.of()
                    : masterDataQueryPort.findAllByPartnerCodes(purchasePartnerCodes);

            // Step 3: 현재 청크 세금계산서 검증 수행
            for (TaxInvoice invoice : chunk) {
                scannedCount++;
                if (!invoice.isPurchaseType()) {
                    continue;
                }
                invoice.validateAmounts();
                if (!partnerMap.containsKey(invoice.getBusinessPartnerCode())) {
                    throw new IllegalStateException(
                            "Business partner missing for tax invoice " + invoice.getIssueId());
                }
                validatedCount++;
            }

            pageNumber++;
        } while (page.hasNext());

        return new TaxInvoiceValidationResult(scannedCount, validatedCount);
    }
}

