package com.ho.account.masterdata.core.application.pipeline;

import com.ho.account.masterdata.batch.application.MasterDataBatchReport;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * 마스터 데이터 유효성 보고서 파이프라인
 */
@Service
public class MasterDataValidityReportPipeline {

    public MasterDataBatchReport createDailyValidityReport(LocalDate asOfDate) {
        // 실제 로직 구현 생략 (컴파일 통과용)
        return new MasterDataBatchReport();
    }
}
