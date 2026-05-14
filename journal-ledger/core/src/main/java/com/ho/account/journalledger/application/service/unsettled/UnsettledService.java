package com.ho.account.journalledger.application.service.unsettled;

import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
 * - UnsettledItemRepository (JPA)를 직접 사용합니다.
 *   (현재 Outbound Port 패턴 미적용 — 추후 헥사고날 전환 시 Port 추가 권장)
 * - 반제 로직은 UnsettledItem.settle() 도메인 메서드에 캡슐화되어 있습니다 (Rich Domain Model).
 *   서비스는 조회 → 도메인 메서드 호출 → 저장의 흐름만 담당합니다.
 * - resolved=false 필터: 미결 항목 목록 조회 시 findByResolvedFalse()를 사용하여
 *   CLEARED(완전 반제)된 항목을 제외합니다.
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class UnsettledService {

    /**
     * 미결 항목 JPA 저장소.
     * unsettled_items 테이블에 접근합니다.
     */
    private final UnsettledItemRepository unsettledItemRepository;

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
        unsettledItemRepository.save(item);
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
     * 반제 처리 로직은 UnsettledItem.settle() 도메인 메서드 내부에 구현됩니다.
     * 반제 금액이 잔액보다 크면 도메인 메서드에서 IllegalArgumentException이 발생합니다.
     *
     * @param id     반제할 미결 항목의 내부 PK
     * @param amount 반제할 금액 (양수여야 하며, 잔액 이하여야 함)
     * @throws IllegalArgumentException 존재하지 않는 미결 항목 ID, 또는 반제 금액 > 잔액
     */
    @Transactional
    public void settleItem(Long id, BigDecimal amount) {
        // 1. 미결 항목 조회 (없으면 즉시 예외)
        UnsettledItem item = unsettledItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unsettled item not found: " + id));

        // 2. 도메인 메서드에 반제 처리 위임 (Rich Domain Model)
        //    settle() 내부에서: settledAmount 누적, remainingAmount 차감, 상태 전환
        item.settle(amount);

        // 3. 변경된 상태 저장
        unsettledItemRepository.save(item);
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
    public List<UnsettledItem> getActiveUnsettledItems() {
        return unsettledItemRepository.findByResolvedFalse();
    }

    /**
     * 특정 거래처의 미결 항목을 조회합니다.
     *
     * [업무 설명]
     * 특정 거래처와의 채권·채무 현황을 확인할 때 사용합니다.
     * 예: 거래처A에게 받아야 할 미수금 목록 조회
     *
     * [개발 설명]
     * 현재 전체 미결 조회 후 인-메모리 필터 방식입니다.
     * 거래처가 많아지면 DB 레벨 필터(JPA 쿼리 추가)로 최적화를 권장합니다.
     * businessPartnerCode가 null이면 전체 미결 항목을 반환합니다.
     *
     * @param businessPartnerCode 거래처 코드 (null이면 전체 반환)
     * @return 해당 거래처의 미결 항목 목록
     */
    public List<UnsettledItem> getUnsettledItems(String businessPartnerCode) {
        List<UnsettledItem> all = unsettledItemRepository.findByResolvedFalse();
        if (businessPartnerCode == null) return all;

        // 거래처 코드로 인-메모리 필터
        return all.stream()
                .filter(item -> businessPartnerCode.equals(item.getBusinessPartnerCode()))
                .collect(Collectors.toList());
    }
}
