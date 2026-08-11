package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [헥사고날 아키텍처 - 전표 전기 REST 인바운드 웹 어댑터 (JournalPostingRestController)]
 *
 * ───────────────────────────────────────────────────────────────────────────────────
 * 🐣 [초보자를 위한 아키텍처 및 MSA 전환 교육용 주석 (Pedagogical Comments)]
 *
 * 1. 모놀리스 직접 결합 제거 후 MSA REST 엔드포인트 정립:
 *    - 기존 As-Is: 모놀리식 환경에서는 타 모듈이 Java 인메모리 메서드 호출(MonolithJournalPostingAdapter)을 사용했습니다.
 *    - To-Be (MSA 전환): `journal-ledger`가 독립적인 Bounded Context(마이크로서비스)로 분리/배포되면,
 *      외부 모듈(Payable, Receivable, Loan, Deposit 등)은 REST HTTP 통신을 통해 전표 생성을 요청합니다.
 *    - 본 컨트롤러는 표준 HTTP REST 엔드포인트(`POST /api/v1/journals/posting`)를 제공하여 
 *      동기 방식의 외부 전표 전기 요청을 수신하는 REST Inbound Web Adapter 역할을 수행합니다.
 *
 * 2. 헥사고날 포트-어댑터 패턴의 연결:
 *    - Controller는 HTTP 요청 바디(JSON)를 계약 DTO인 {@link JournalEntryCommand}로 수신합니다.
 *    - 내부 비즈니스 로직 및 유즈케이스 호출은 직접 서비스 클래스를 알 필요 없이 
 *      포트 인터페이스인 {@link JournalPostingPort}를 거쳐 수행됩니다.
 *    - 이를 통해 웹 계층과 핵심 도메인 로직이 느슨하게 결합(Loosely Coupled)되어 테스트 및 유지보수가 유연해집니다.
 * ───────────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/journals/posting")
@RequiredArgsConstructor
public class JournalPostingRestController {

    private final JournalPostingPort journalPostingPort;

    /**
     * 외부 마이크로서비스로부터 전표 전기(생성 초안) 요청을 수신합니다.
     *
     * @param command 표준 전표 생성 커맨드 계약 객체
     * @return 전표 생성 결과 (전표 ID, 전표 번호, 상태)
     */
    @PostMapping
    public ResponseEntity<JournalPostingResult> createPosting(@Valid @RequestBody JournalEntryCommand command) {
        log.info("Received REST Journal Posting request for lineageSourceId: {}, type: {}",
                command.lineageSourceId(), command.lineageSourceType());
        
        try {
            JournalPostingResult result = journalPostingPort.createDraftEntry(command);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid journal entry command: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("Journal creation failed due to state rule: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
