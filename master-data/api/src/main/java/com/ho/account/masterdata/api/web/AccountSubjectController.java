package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.AccountSubjectDto;
import com.ho.account.masterdata.api.dto.AccountSubjectRequestDto;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/basic/account-subjects")
public class AccountSubjectController {

    private final AccountSubjectUseCase accountSubjectUseCase;

    public AccountSubjectController(AccountSubjectUseCase accountSubjectUseCase) {
        this.accountSubjectUseCase = accountSubjectUseCase;
    }

    @PostMapping
    public ResponseEntity<AccountSubjectDto> createAccountSubject(
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles,
            @RequestBody AccountSubjectRequestDto requestDto) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        try {
            return ResponseEntity.ok(AccountSubjectDto.fromEntity(
                    accountSubjectUseCase.createAccountSubject(requestDto.toCommand())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public List<AccountSubjectDto> getActiveAccountSubjects() {
        return accountSubjectUseCase.findAllActiveAccountSubjects().stream()
                .map(AccountSubjectDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{code}")
    public ResponseEntity<AccountSubjectDto> getAccountSubjectByCode(@PathVariable String code) {
        return accountSubjectUseCase.findAccountSubjectByCode(code)
                .map(account -> ResponseEntity.ok(AccountSubjectDto.fromEntity(account)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{code}")
    public ResponseEntity<AccountSubjectDto> updateAccountSubject(
            @PathVariable String code,
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles,
            @RequestBody AccountSubjectRequestDto requestDto) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        try {
            return ResponseEntity.ok(
                    AccountSubjectDto.fromEntity(accountSubjectUseCase.updateAccountSubject(code, requestDto.toCommand())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deactivateAccountSubject(
            @PathVariable String code,
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        try {
            accountSubjectUseCase.deactivateAccountSubject(code);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
