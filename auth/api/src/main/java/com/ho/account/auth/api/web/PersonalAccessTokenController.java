package com.ho.account.auth.api.web;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.application.exception.PatAccessDeniedException;
import com.ho.account.auth.core.application.exception.PatAuthenticationException;
import com.ho.account.auth.core.application.port.in.PersonalAccessTokenUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 🐣 [개인용 액세스 토큰 (PAT) 관리 컨트롤러]
 * 
 * - 사용자가 자신의 마이페이지에서 AI 에이전트/스크립트용 전용 키를 직접 생성하고,
 * - 관리자(System Admin)가 전사 생성된 PAT를 모니터링하고 강제 폐기(Revoke)할 수 있는 API입니다.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PersonalAccessTokenController {

    private final PersonalAccessTokenUseCase patUseCase;

    public record CreatePatRequest(String username, String tokenName, Integer expireDays) {}

    /**
     * 1. 개인용 액세스 토큰(PAT) 신규 생성
     *    (원문 토큰 pat_live_... 은 보안상 생성 직후 응답에서 1회만 제공)
     */
    @PostMapping("/pat")
    public ResponseEntity<PersonalAccessTokenCreateResponse> createPat(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody CreatePatRequest request) {
        // The legacy username field is accepted for compatibility, but never selects the PAT owner.
        String tokenName = (request.tokenName() != null && !request.tokenName().isBlank()) ? request.tokenName() : "My AI Agent Key";
        int days = (request.expireDays() != null && request.expireDays() > 0) ? request.expireDays() : 90;

        PersonalAccessTokenCreateResponse response = patUseCase.createToken(authorization, tokenName, days);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 내 PAT 토큰 목록 조회
     */
    @GetMapping("/pat")
    public ResponseEntity<List<PersonalAccessTokenDto>> getMyPats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        List<PersonalAccessTokenDto> list = patUseCase.getUserTokens(authorization);
        return ResponseEntity.ok(list);
    }

    /**
     * 3. 내 PAT 토큰 폐기 (Revoke)
     */
    @DeleteMapping("/pat/{id}")
    public ResponseEntity<Void> revokeMyPat(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        boolean success = patUseCase.revokeOwnToken(authorization, id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * 4. [관리자 전용] 전사 모든 사용자 PAT 토큰 감시 조회
     */
    @GetMapping("/admin/pat")
    public ResponseEntity<List<PersonalAccessTokenDto>> getAllPatsForAdmin(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        List<PersonalAccessTokenDto> list = patUseCase.getAllTokensForAdmin(authorization);
        return ResponseEntity.ok(list);
    }

    /**
     * 5. [관리자 전용] 특정 사용자 PAT 토큰 강제 폐기 (Admin Force Revoke)
     */
    @DeleteMapping("/admin/pat/{id}")
    public ResponseEntity<Void> forceRevokePatByAdmin(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        boolean success = patUseCase.revokeTokenForAdmin(authorization, id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(PatAuthenticationException.class)
    public ResponseEntity<Void> handlePatAuthentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(PatAccessDeniedException.class)
    public ResponseEntity<Void> handlePatAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
