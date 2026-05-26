package com.risk.credit.api.controller;

import com.risk.credit.core.application.service.calculation.EadBatchService;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * [API] EAD(부도시 익스포저) 산출 및 배치 제어 컨트롤러.
 * 부도 시점의 예상 노출 금액(EAD)을 대량으로 산출하는 배치 프로세스를 제어합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * ============================================================
 * EAD(Exposure at Default)란 고객이 실제로 부도가 났을 때 "은행이 잃을 수 있는 총 금액"을 말합니다.
 *
 * 계산 공식: EAD = 현재 미상환잔액(Outstanding) + 미사용한도 × CCF
 *   - Outstanding: 고객이 현재 실제로 빌려간 돈 (예: 대출금)
 *   - 미사용한도(Undrawn): 고객이 아직 쓰지 않았지만 언제든지 꺼낼 수 있는 한도 (예: 마이너스 통장)
 *   - CCF(Credit Conversion Factor, 신용환산율): 미사용한도 중에서 실제로 쓸 가능성을 %로 나타낸 값
 *
 * 예시: 대출한도 1억, 현재 잔액 6천만원, CCF 80%
 *   EAD = 6천만 + (1억 - 6천만) × 80% = 6천만 + 3,200만 = 9,200만원
 *
 * 이 컨트롤러는 수천, 수만 건의 계좌를 한꺼번에 계산하는 '배치 엔진'을 가동시키는 역할을 합니다.
 * ============================================================
 *
 * 🔗 API 라우팅:
 *   - POST /api/v1/credit-risk/ead/batch         → 표준 EAD 배치 실행
 *   - POST /api/v1/credit-risk/batch/run-ead     → 프론트엔드 호환용 별칭 (동일한 비즈니스 로직 수행)
 *   - GET  /api/v1/credit-risk/ead/verification  → 산출 결과 검증 목록 조회
 */
@RestController
@RequestMapping("/api/v1/credit-risk/ead")
@RequiredArgsConstructor
public class EadController {

    // EadBatchService: EAD 산출 비즈니스 로직을 담당하는 서비스 (credit-core 모듈에 정의됨)
    private final EadBatchService batchService;

    /**
     * [POST] 특정 기준일의 EAD 산출 배치를 실행한다.
     *
     * 💡 [왕초보 가이드] HTTP POST 메서드의 의미
     *   GET = "조회해줘" (데이터를 가져옴)
     *   POST = "실행해줘" (새로운 작업을 시작함)
     *   이 엔드포인트는 배치를 '실행'하므로 POST를 사용합니다.
     *
     * 기본 경로: /api/v1/credit-risk/ead/batch
     *
     * @param baseDate 기준일자 (YYYY-MM-DD 형식, 예: 2026-04-18)
     * @return 산출된 EAD 결과 목록
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<CrRiskResult>>> triggerBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        // batchService.executeBatch: 해당 날짜의 모든 활성 계좌에 대해 EAD를 계산합니다.
        return ResponseEntity.ok(ApiResponse.success(batchService.executeBatch(baseDate)));
    }

    /**
     * [POST] /api/v1/credit-risk/batch/run-ead - 프론트엔드 호환용 별칭 엔드포인트.
     *
     * 💡 [왕초보 가이드] 왜 이 엔드포인트가 따로 필요한가?
     *   프론트엔드(Next.js)에서 배치 실행 버튼을 클릭하면
     *   '/api/v1/credit-risk/batch/run-ead' 경로로 요청을 보냅니다.
     *   이 경로를 여기에 추가하여 같은 비즈니스 로직(/ead/batch)을 수행하도록 연결합니다.
     *   실제 서비스 로직을 복사하지 않고 기존 서비스를 재사용하므로 중복 코드가 없습니다.
     *
     * ⚠️ [아키텍처 노트] 이 메서드는 URL 매핑 경로(_RequestMapping_)가 다른 클래스
     *   (CrBatchController)에 속하는 것이 더 어울립니다.
     *   현재는 프론트엔드 URL 불일치 문제를 신속히 해결하기 위한 임시 조치입니다.
     *
     * @param baseDate 기준일자 (YYYY-MM-DD 형식)
     */
    @PostMapping({"/run-ead-alt"}) // 별칭: CrBatchController의 /batch/run-ead에서 호출됨
    public ResponseEntity<ApiResponse<List<CrRiskResult>>> triggerBatchAlias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ResponseEntity.ok(ApiResponse.success(batchService.executeBatch(baseDate)));
    }

    /**
     * [GET] 산출된 EAD 데이터의 검증을 위한 페이징 목록을 조회한다.
     *
     * 💡 [왕초보 가이드] 페이징(Paging)이란?
     *   EAD 산출 결과가 수만 건이라면, 한 번에 전부 보여주면 화면이 느려집니다.
     *   이를 방지하기 위해 '페이지(page)' 단위로 조금씩 나누어 보여주는 기법입니다.
     *   예: page=0, size=20이면 1번째~20번째 행을, page=1이면 21~40번째 행을 가져옵니다.
     *
     * @param baseDate 기준일자
     * @param page     페이지 번호 (0부터 시작, 기본값: 0 = 첫 번째 페이지)
     * @param size     페이지당 행 수 (기본값: 20)
     * @return 페이징된 EAD 결과 목록
     */
    @GetMapping("/verification")
    public ResponseEntity<ApiResponse<Page<CrRiskResult>>> getVerificationData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // PageRequest.of(page, size): JPA에게 "몇 번째 페이지, 몇 개씩 가져와"를 알려주는 객체입니다.
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                batchService.getResultsForVerification(baseDate, pageRequest)));
    }
}
