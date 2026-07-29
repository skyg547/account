package com.ho.account.auth.api.web;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.application.service.PersonalAccessTokenService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    private final PersonalAccessTokenService patService;

    public record CreatePatRequest(String username, String tokenName, Integer expireDays) {}

    /**
     * 1. 개인용 액세스 토큰(PAT) 신규 생성
     *    (원문 토큰 pat_live_... 은 보안상 생성 직후 응답에서 1회만 제공)
     */
    @PostMapping("/pat")
    public ResponseEntity<PersonalAccessTokenCreateResponse> createPat(@RequestBody CreatePatRequest request) {
        String username = (request.username() != null && !request.username().isBlank()) ? request.username() : "admin";
        String tokenName = (request.tokenName() != null && !request.tokenName().isBlank()) ? request.tokenName() : "My AI Agent Key";
        int days = (request.expireDays() != null && request.expireDays() > 0) ? request.expireDays() : 90;

        PersonalAccessTokenCreateResponse response = patService.createToken(username, tokenName, days);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 내 PAT 토큰 목록 조회
     */
    @GetMapping("/pat")
    public ResponseEntity<List<PersonalAccessTokenDto>> getMyPats(@RequestParam(defaultValue = "admin") String username) {
        List<PersonalAccessTokenDto> list = patService.getUserTokens(username);
        return ResponseEntity.ok(list);
    }

    /**
     * 3. 내 PAT 토큰 폐기 (Revoke)
     */
    @DeleteMapping("/pat/{id}")
    public ResponseEntity<Void> revokeMyPat(
            @PathVariable String id,
            @RequestParam(defaultValue = "admin") String username) {
        boolean success = patService.revokeToken(username, id, false);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * 4. [관리자 전용] 전사 모든 사용자 PAT 토큰 감시 조회
     */
    @GetMapping("/admin/pat")
    public ResponseEntity<List<PersonalAccessTokenDto>> getAllPatsForAdmin() {
        List<PersonalAccessTokenDto> list = patService.getAllTokensForAdmin();
        return ResponseEntity.ok(list);
    }

    /**
     * 5. [관리자 전용] 특정 사용자 PAT 토큰 강제 폐기 (Admin Force Revoke)
     */
    @DeleteMapping("/admin/pat/{id}")
    public ResponseEntity<Void> forceRevokePatByAdmin(@PathVariable String id) {
        boolean success = patService.revokeToken("admin", id, true);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
