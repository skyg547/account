package com.risk.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [Source] 원천 계정과목 마스터 (Chart of Accounts) 엔티티.
 * 은행 내부의 전 계정과목에 대한 분류 정보 및 리스크 관리 기준을 정의합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 테이블은 은행의 모든 통장에 붙는 '꼬리표' 같은 것입니다.
 * "이 계좌는 자산(내 돈)인가 부채(빌린 돈)인가 ", "개인 고객용인가 기업 고객용인가 " 등을 결정합니다.
 * 리스크 시스템은 이 정보를 보고 각 계좌를 어떻게 분석할지(예: 대출로 볼 것인지, 유동성 부채로 볼 것인지)를 판단합니다.
 */
@Entity
@Table(name = "ods_acc_mst")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdsAccountMstEntity {

    /** 계정과목 코드 - 회계 시스템에서 사용하는 고유 식별 번호입니다. */
    @Id
    @Column(name = "subj_cd", length = 20)
    private String subjectCode;

    /** 계정과목 명칭 - 사람이 이해할 수 있는 계정명입니다 (예: 일반가계대출, 요구불예금). */
    @Column(name = "subj_nm", length = 100)
    private String subjectName;

    /** 계정 유형 (예: DEMAND: 요구불, TIME: 저축성, LOAN: 대출 등) */
    @Column(name = "acc_type", length = 20)
    private String accountType;

    /** B/S 분류 (ASSET: 자산, LIABILITY: 부채, EQUITY: 자본, OFF_BS: 난외자산) */
    @Column(name = "bs_class", length = 10)
    private String bsClass;

    /** 자산 여부 - 은행이 타인에게 돈을 빌려주어 받을 권리가 있는 항목이면 True입니다. */
    @Column(name = "is_asset")
    private Boolean isAsset;

    /** 사업부 코드 (예: RETAIL: 가계금융, CORPORATE: 기업금융, WM: 자산관리 등) */
    @Column(name = "biz_unit_cd", length = 10)
    private String bizUnitCd;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
