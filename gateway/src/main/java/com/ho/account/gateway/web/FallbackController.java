package com.ho.account.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 서킷 브레이커 폴백(Fallback) 컨트롤러
 * 뒷단 마이크로서비스가 죽었거나, 응답이 너무 오래 걸려 두꺼비집이 내려갔을 때
 * 고객에게 흉측한 500 에러 화면 대신 친절한 안내 메시지를 보내는 "비상 대피소" 입니다.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/master-data")
    public Mono<ResponseEntity<Map<String, Object>>> masterDataFallback() {
        log.error("[서킷 브레이커 발생] Master Data 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 기준정보(Master Data) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/journal-ledger")
    public Mono<ResponseEntity<Map<String, Object>>> journalLedgerFallback() {
        log.error("[서킷 브레이커 발생] Journal Ledger 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 전표/원장(Journal/Ledger) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/closing")
    public Mono<ResponseEntity<Map<String, Object>>> closingFallback() {
        log.error("[서킷 브레이커 발생] Closing 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 결산(Closing) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Gateway의 폴백은 예산 계산이나 상태 전이를 대신하지 않고, Budget API 장애를 HTTP 표현으로만
     * 변환합니다. 따라서 업무 원장은 Budget 서비스에 그대로 남고 호출자는 503을 보고 안전하게
     * 재시도 여부를 결정할 수 있습니다.
     */
    @RequestMapping("/budget")
    public Mono<ResponseEntity<Map<String, Object>>> budgetFallback() {
        log.error("[서킷 브레이커 발생] Budget 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 예산(Budget) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/internal-audit")
    public Mono<ResponseEntity<Map<String, Object>>> internalAuditFallback() {
        log.error("[Circuit breaker] Internal Audit service is unavailable.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "Internal Audit service is temporarily unavailable.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/deposit")
    public Mono<ResponseEntity<Map<String, Object>>> depositFallback() {
        log.error("[서킷 브레이커 발생] Deposit 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 입금(Deposit) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/receivable")
    public Mono<ResponseEntity<Map<String, Object>>> receivableFallback() {
        log.error("[서킷 브레이커 발생] Receivable 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 채권(Receivable) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/payable")
    public Mono<ResponseEntity<Map<String, Object>>> payableFallback() {
        log.error("[서킷 브레이커 발생] Payable 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 채무(Payable) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/tax")
    public Mono<ResponseEntity<Map<String, Object>>> taxFallback() {
        log.error("[서킷 브레이커 발생] Tax 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 세무(Tax) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/reconciliation")
    public Mono<ResponseEntity<Map<String, Object>>> reconciliationFallback() {
        log.error("[서킷 브레이커 발생] Reconciliation 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 대사(Reconciliation) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/asset-lease")
    public Mono<ResponseEntity<Map<String, Object>>> assetLeaseFallback() {
        log.error("[서킷 브레이커 발생] Asset Lease 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 자산/리스(Asset/Lease) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/reporting")
    public Mono<ResponseEntity<Map<String, Object>>> reportingFallback() {
        log.error("[서킷 브레이커 발생] Reporting 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 보고서(Reporting) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/expenditure")
    public Mono<ResponseEntity<Map<String, Object>>> expenditureFallback() {
        log.error("[서킷 브레이커 발생] Expenditure Resolution 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 지출결의(Expenditure Resolution) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping({"/mart", "/account-mart"})
    public Mono<ResponseEntity<Map<String, Object>>> martFallback() {
        log.error("[서킷 브레이커 발생] Account Mart 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 데이터마트(Account Mart) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/ecl")
    public Mono<ResponseEntity<Map<String, Object>>> eclFallback() {
        log.error("[서킷 브레이커 발생] ECL 서비스에 연결할 수 없습니다.");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "현재 기대신용손실(ECL) 시스템 점검 중이거나 장애가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
