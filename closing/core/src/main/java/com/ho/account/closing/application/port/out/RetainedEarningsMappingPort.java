package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ApprovedRetainedEarningsMapping;

/**
 * 회계연도별 승인된 이익잉여금 대체 계정 통제를 조회하는 아웃바운드 포트입니다.
 */
public interface RetainedEarningsMappingPort {

    ApprovedRetainedEarningsMapping requireForYear(int fiscalYear);
}
