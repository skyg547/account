package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.api.dto.AccountSubjectDto;
import com.ho.account.masterdata.api.dto.AccountSubjectRequestDto;
import com.ho.account.masterdata.core.application.usecase.AccountSubjectUseCase;
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
    public ResponseEntity<AccountSubjectDto> createAccountSubject(@RequestBody AccountSubjectRequestDto requestDto) {
        try {
            AccountSubject createdAccount = accountSubjectUseCase.createAccountSubject(requestDto.toCommand());
            return ResponseEntity.ok(AccountSubjectDto.fromEntity(createdAccount));
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
    public ResponseEntity<AccountSubjectDto> updateAccountSubject(@PathVariable String code, @RequestBody AccountSubjectRequestDto requestDto) {
        try {
            AccountSubject updatedAccount = accountSubjectUseCase.updateAccountSubject(code, requestDto.toCommand());
            return ResponseEntity.ok(AccountSubjectDto.fromEntity(updatedAccount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deactivateAccountSubject(@PathVariable String code) {
        try {
            accountSubjectUseCase.deactivateAccountSubject(code);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
