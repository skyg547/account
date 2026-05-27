package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.exposure.CrAccount;
import java.util.List;

/**
 * [Port] 계좌 대량 처리를 위한 아웃고잉 포트
 */
public interface CrAccountBulkPort {
    
    /**
     * 연체 일수에 따른 스테이징 일괄 업데이트
     */
    int updateStagingByDelinquentDays(int lowerDays, Integer upperDays, String targetStage);

    /**
     * 조기경보 레벨에 따른 스테이징 일괄 업데이트
     */
    int updateStagingByWarningLevel(String warningLevel, String targetStage);

    /**
     * 데이터 품질 오류 계좌 일괄 마킹 (Identity 정보 누락)
     */
    int markInvalidIdentityAccounts();

    /**
     * 데이터 품질 오류 계좌 일괄 마킹 (리스크 파라미터 누락)
     */
    int markInvalidRiskParamAccounts();

    /**
     * 데이터 품질 오류 계좌 일괄 마킹 (비정상 가액)
     */
    int markInvalidAmountAccounts();

    /**
     * 오류 메시지 일괄 초기화
     */
    void clearErrorMessages();

    /**
     * [고도화] 데이터 마트 동기화를 위한 대량 Upsert
     */
    void bulkUpsert(List<CrAccount> accounts);
}
