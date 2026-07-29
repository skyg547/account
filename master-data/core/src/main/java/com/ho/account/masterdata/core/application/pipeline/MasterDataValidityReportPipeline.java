package com.ho.account.masterdata.core.application.pipeline;

import com.ho.account.masterdata.core.application.port.out.MasterDataValidityStatisticsPort;
import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 마스터 데이터 유효성 보고서 파이프라인
 *
 * <p>초보자 설명: 이 파이프라인은 "어떤 기준일의 활성 건수를 모을지"만 결정합니다.
 * 실제 대용량 집계는 출력 포트 뒤의 DB 어댑터가 COUNT 쿼리로 처리합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class MasterDataValidityReportPipeline {

    private final MasterDataValidityStatisticsPort statisticsPort;

    public MasterDataValidityReport createDailyValidityReport(LocalDate asOfDate) {
        LocalDate reportDate = Objects.requireNonNull(
                asOfDate,
                "asOfDate is required so that the validity report can be reproduced.");

        return new MasterDataValidityReport(
                reportDate,
                statisticsPort.countActiveAccountSubjects(reportDate),
                statisticsPort.countActiveDepartments(reportDate),
                statisticsPort.countActiveProducts(reportDate),
                statisticsPort.countActiveBusinessPartners(reportDate));
    }
}