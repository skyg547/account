package com.ho.account.journalledger.application.service.unsettled;

import com.ho.account.journalledger.application.port.in.UnsettledItemUseCase;
import com.ho.account.journalledger.application.port.out.UnsettledItemPersistencePort;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 미결 항목 관리 서비스 (Unsettled Service).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 미결(未決) 항목이란 거래는 발생했지만 아직 결제(반제)가 완료되지 않은 채권·채무입니다.
 *
 * 예시:
 *   - 외상 매출 발생: 거래처A에게 물건을 팔았지만 아직 대금을 못 받음 → 미결 채권 등록
 *   - 외상 매입 발생: 거래처B에서 물건을 샀지만 아직 대금을 안 냄 → 미결 채무 등록
 *   - 선급금 지급: 서비스 대금을 미리 지급했지만 아직 납품이 안 됨 → 미결 선급금 등록
 *
 * 반제(Settlement) 처리:
 *   거래처로부터 대금을 수령하거나, 거래처에 대금을 지급하면 반제 처리합니다.
 *   반제 상태 흐름:
 *     [OPEN] → 일부 반제 → [PARTIAL] → 나머지 반제 → [CLEARED]
 *   CLEARED가 되면 resolved=true로 설정되어 미결 목록에서 제외됩니다.
 *
 * 이 서비스가 담당하는 업무:
 *   1. registerUnsettledItem(): 미결 항목 신규 등록 (전표 전기 시 호출)
 *   2. settleItem(): 반제 처리 (대금 수령/지급 시 호출)
 *   3. getActiveUnsettledItems(): 미결(OPEN/PARTIAL) 항목 전체 조회
 *   4. getUnsettledItems(거래처코드): 특정 거래처의 미결 항목 조회
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - 외부 HTTP 어댑터는 UnsettledItemUseCase 인바운드 포트에만 의존합니다.
 * - 이 서비스는 UnsettledItemPersistencePort 출력 포트에만 의존하며,
 *   실제 JPA 조회와 저장은 UnsettledItemPersistenceAdapter가 담당합니다.
 * - 반제 로직은 UnsettledItem.settle() 도메인 메서드에 캡슐화되어 있습니다 (Rich Domain Model).
 *   서비스는 트랜잭션 안에서 독점 점유 → 최신 상태 조회 → 도메인 메서드 호출 → 저장을
 *   조정하고, 성공 시 함께 커밋합니다.
 * - 반제 중 예외가 발생하면 점유와 상태 변경은 함께 rollback됩니다. 잠금 실패나 deadlock을
 *   재시도할 때는 rollback이 끝난 뒤 유즈케이스 전체를 새 트랜잭션으로 다시 호출해야 합니다.
 * - resolved=false 필터: 미결 항목 목록 조회 시 findByResolvedFalse()를 사용하여
 *   CLEARED(완전 반제)된 항목을 제외합니다.
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class UnsettledService implements UnsettledItemUseCase {

    /**
     * 미결 항목 영속성 출력 포트.
     * 서비스는 DB 기술을 모르고 미결 항목의 저장·조회 의도만 전달합니다.
     */
    private final UnsettledItemPersistencePort unsettledItemPersistencePort;

    /**
     * 미결 항목을 신규 등록합니다.
     *
     * [업무 설명]
     * 전표가 전기될 때, 채권·채무가 발생하는 계정과목(매출채권, 매입채무 등)에 대해
     * 미결 항목을 자동으로 등록합니다.
     * 초기 상태는 OPEN(완전 미결)이며, managementNo가 자동 생성됩니다("UNS-XXXXXXXX" 형식).
     *
     * [개발 설명]
     * UnsettledItem의 @PrePersist에서 managementNo 자동 생성, status="OPEN" 초기화가 이루어집니다.
     * 이 메서드는 단순히 도메인 객체를 받아 저장하는 역할입니다.
     *
     * @param item 등록할 미결 항목 도메인 객체
     *             (accountSubject, businessPartner, originalAmount 등이 설정된 상태)
     */
    @Transactional
    public void registerUnsettledItem(UnsettledItem item) {
        unsettledItemPersistencePort.save(item);
    }

    /**
     * 미결 항목에 반제를 처리합니다.
     *
     * [업무 설명]
     * 거래처로부터 대금을 수령하거나, 거래처에 대금을 지급했을 때 호출합니다.
     * 반제 금액만큼 settledAmount가 증가하고, remainingAmount가 감소합니다.
     *
     * 예시:
     *   미결 채권 100,000원 (OPEN)
     *   → settleItem(id, 60,000원) 호출
     *   → settledAmount=60,000, remainingAmount=40,000, status=PARTIAL
     *   → settleItem(id, 40,000원) 호출
     *   → settledAmount=100,000, remainingAmount=0, status=CLEARED, resolved=true
     *
     * [개발 설명]
     * ID·금액·처리자·참조번호 입력을 받은 뒤 출력 포트가 현재 미결 항목을 트랜잭션 범위로
     * 독점 점유하고 최신 상태를 반환합니다. UnsettledItem.settle()이 정밀도·참조번호 멱등성·
     * 상태 전이를 검증하며, 서비스는 그 결과를 같은 트랜잭션에서 저장하고 커밋합니다.
     * 반제 금액이 잔액보다 크거나 다른 검증이 실패하면 예외로 전체 트랜잭션이 rollback됩니다.
     * 동시성 실패 재시도는 새 트랜잭션에서 이 입력부터 다시 실행해야 최신 잔액과 참조 이력을
     * 다시 읽습니다.
     *
     * @param id                  반제할 미결 항목의 내부 PK
     * @param amount              반제할 금액 (양수여야 하며, 잔액 이하여야 함)
     * @param actor               실제 반제 처리자
     * @param settlementReference 은행 거래번호 등 재요청 중복을 판별할 참조번호
     * @throws IllegalArgumentException 존재하지 않는 미결 항목 ID, 또는 반제 금액 > 잔액
     */
    @Transactional
    @Override
    public void settleItem(Long id, BigDecimal amount, String actor, String settlementReference) {
        // 최신 잔액과 참조 이력을 기준으로 도메인 결정을 내리도록 commit/rollback까지 점유합니다.
        UnsettledItem item = unsettledItemPersistencePort.findByIdForSettlement(id)
                .orElseThrow(() -> new IllegalArgumentException("Unsettled item not found: " + id));

        item.settle(amount, actor, settlementReference);

        unsettledItemPersistencePort.save(item);
    }

    /**
     * 아직 완전히 반제되지 않은 모든 미결 항목을 조회합니다.
     *
     * [업무 설명]
     * 현재 미결(미수금, 미지급금) 현황을 확인할 때 사용합니다.
     * OPEN(전액 미결)과 PARTIAL(일부 반제) 상태 항목이 포함됩니다.
     * CLEARED(완전 반제) 항목은 제외됩니다.
     *
     * [개발 설명]
     * resolved=false인 항목만 조회합니다.
     * resolved 필드는 status=="CLEARED"이면 true로 자동 설정되므로,
     * CLEARED된 항목은 이 조회에서 자동 제외됩니다.
     *
     * @return 미결(OPEN/PARTIAL) 상태의 모든 미결 항목 목록
     */
    @Transactional(readOnly = true)
    public List<UnsettledItem> getActiveUnsettledItems() {
        return unsettledItemPersistencePort.findActive();
    }

    /**
     * 특정 거래처의 미결 항목을 조회합니다.
     *
     * [업무 설명]
     * 특정 거래처와의 채권·채무 현황을 확인할 때 사용합니다.
     * 예: 거래처A에게 받아야 할 미수금 목록 조회
     *
     * [개발 설명]
     * 거래처 코드가 있으면 출력 포트가 DB 조건으로 해당 거래처의 미결 항목만 조회합니다.
     * 따라서 거래처가 많아져도 전체 미결 데이터를 애플리케이션 메모리에 올리지 않습니다.
     * businessPartnerCode가 null 또는 공백이면 전체 미결 항목을 반환합니다.
     *
     * @param businessPartnerCode 거래처 코드 (null이면 전체 반환)
     * @return 해당 거래처의 미결 항목 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<UnsettledItem> getUnsettledItems(String businessPartnerCode) {
        if (businessPartnerCode == null || businessPartnerCode.isBlank()) {
            return unsettledItemPersistencePort.findActive();
        }
        return unsettledItemPersistencePort.findActiveByBusinessPartnerCode(businessPartnerCode.trim());
    }
}
