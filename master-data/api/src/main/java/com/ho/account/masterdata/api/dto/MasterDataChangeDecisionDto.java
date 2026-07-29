package com.ho.account.masterdata.api.dto;

import jakarta.validation.constraints.Size;

/**
 * 반려 사유를 전달하는 HTTP 입력 DTO입니다.
 *
 * <p>결정자는 요청 본문을 신뢰하지 않고 Gateway가 검증한 {@code X-Auth-User}에서 받습니다.</p>
 */
public class MasterDataChangeDecisionDto {

    @Size(max = 500)
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
