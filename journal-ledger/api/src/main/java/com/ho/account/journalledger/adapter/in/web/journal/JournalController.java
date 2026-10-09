package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.out.SlipNumberAllocationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 전표 관리 API 컨트롤러
 * 
 * 🐣 [초보자를 위한 설명]
 * 사용자가 화면에서 수동 전표를 입력하거나, ERP/Banking 모듈에서 거래가 발생하여 전표를 자동 생성할 때
 * 이 컨트롤러가 API 요청을 받아줍니다. HTTP 요청을 애플리케이션 서비스(JournalUseCase)로 전달하여 
 * 비즈니스 로직을 실행하도록 돕는 '문지기' 역할을 합니다.
 */
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalUseCase journalUseCase;

    /**
     * 수동 전표 생성
     */
    @PostMapping
    public ResponseEntity<JournalApiDto.View> createJournalEntry(
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles,
            @Valid @RequestBody JournalApiDto.CreateRequest request) {
        String maker = JournalCommandAuthorization.requireMaker(actor, roles);
        try {
            return ResponseEntity.ok(JournalApiDto.View.from(
                    journalUseCase.createJournalEntry(request.toDomain(maker))));
        } catch (SlipNumberAllocationException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 이벤트에 의한 전표 자동 생성 (룰 엔진 사용)
     */
    @PostMapping("/from-event")
    public ResponseEntity<JournalApiDto.View> createJournalEntryFromEvent(
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles,
            @Valid @RequestBody JournalApiDto.EventRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) {
        String maker = JournalCommandAuthorization.requireMaker(actor, roles);
        try {
            return journalUseCase.createJournalEntryFromEvent(request.eventDataWithActor(maker), accountingDate)
                    .map(JournalApiDto.View::from)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (SlipNumberAllocationException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 기간별 전표 목록 조회
     */
    @GetMapping
    public List<JournalApiDto.View> getJournalEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return journalUseCase.getJournalEntriesByDate(startDate, endDate).stream()
                .map(JournalApiDto.View::from)
                .toList();
    }

    /**
     * 전표 번호로 상세 조회
     */
    @GetMapping("/{slipNo}")
    public ResponseEntity<JournalApiDto.View> getJournalEntry(@PathVariable String slipNo) {
        return journalUseCase.getJournalEntryBySlipNo(slipNo)
                .map(JournalApiDto.View::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Stable identifier lookup including lines, used to validate closing reruns. */
    @GetMapping("/by-id/{id}")
    public ResponseEntity<JournalApiDto.View> getJournalEntryById(@PathVariable Long id) {
        if (id < 1) return ResponseEntity.badRequest().build();
        return journalUseCase.getJournalEntryWithDetails(id)
                .map(JournalApiDto.View::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 전표 승인 요청
     */
    @PostMapping("/{id}/request-approval")
    public ResponseEntity<Void> requestJournalEntryApproval(
            @PathVariable Long id,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles) {
        String maker = JournalCommandAuthorization.requireMaker(actor, roles);
        try {
            journalUseCase.requestJournalEntryApproval(id, maker);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 전표 승인
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveJournalEntry(
            @PathVariable Long id,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles) {
        String approver = JournalCommandAuthorization.requireApprover(actor, roles);
        try {
            journalUseCase.approveJournalEntry(id, approver);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 전표 전기 (원장 반영)
     */
    @PostMapping("/{id}/post")
    public ResponseEntity<Void> postJournalEntry(
            @PathVariable Long id,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = JournalCommandAuthorization.AUTH_ROLES_HEADER, required = false) String roles) {
        String poster = JournalCommandAuthorization.requirePoster(actor, roles);
        try {
            journalUseCase.postJournalEntry(id, poster);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
