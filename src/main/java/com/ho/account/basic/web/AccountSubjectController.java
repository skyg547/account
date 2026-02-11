package com.ho.account.basic.web;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.dto.AccountSubjectDto;
import com.ho.account.basic.dto.AccountSubjectRequestDto;
import com.ho.account.basic.service.AccountSubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/basic/account-subjects")
public class AccountSubjectController {

    private final AccountSubjectService accountSubjectService;

    @Autowired
    public AccountSubjectController(AccountSubjectService accountSubjectService) {
        this.accountSubjectService = accountSubjectService;
    }

    @PostMapping
    public ResponseEntity<AccountSubjectDto> createAccountSubject(@RequestBody AccountSubjectRequestDto requestDto) {
        try {
            AccountSubject createdAccount = accountSubjectService.createAccountSubject(requestDto);
            return ResponseEntity.ok(AccountSubjectDto.fromEntity(createdAccount));
        } catch (IllegalArgumentException e) {
            // Consider creating a proper error response object
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public List<AccountSubjectDto> getActiveAccountSubjects() {
        return accountSubjectService.findAllActiveAccountSubjects().stream()
                .map(AccountSubjectDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{code}")
    public ResponseEntity<AccountSubjectDto> getAccountSubjectByCode(@PathVariable String code) {
        return accountSubjectService.findAccountSubjectByCode(code)
                .map(account -> ResponseEntity.ok(AccountSubjectDto.fromEntity(account)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{code}")
    public ResponseEntity<AccountSubjectDto> updateAccountSubject(@PathVariable String code, @RequestBody AccountSubjectRequestDto requestDto) {
        try {
            AccountSubject updatedAccount = accountSubjectService.updateAccountSubject(code, requestDto);
            return ResponseEntity.ok(AccountSubjectDto.fromEntity(updatedAccount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deactivateAccountSubject(@PathVariable String code) {
        try {
            accountSubjectService.deactivateAccountSubject(code);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
