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
}
