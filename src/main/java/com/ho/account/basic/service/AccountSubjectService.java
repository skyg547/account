package com.ho.account.basic.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.dto.AccountSubjectRequestDto;
import com.ho.account.basic.repository.AccountSubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 계정과목 마스터 데이터에 대한 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@Transactional
public class AccountSubjectService {

    private final AccountSubjectRepository accountSubjectRepository;

    @Autowired
    public AccountSubjectService(AccountSubjectRepository accountSubjectRepository) {
        this.accountSubjectRepository = accountSubjectRepository;
    }

    /**
     * 새로운 계정과목을 생성합니다.
     * @param requestDto 생성할 계정과목 정보가 담긴 DTO
     * @return 저장된 계정과목 엔티티
     */
    public AccountSubject createAccountSubject(AccountSubjectRequestDto requestDto) {
        if (accountSubjectRepository.existsById(requestDto.getCode())) {
            throw new IllegalArgumentException("이미 존재하는 계정 코드입니다: " + requestDto.getCode());
        }

        AccountSubject accountSubject = requestDto.toEntity();

        // parentCode로 부모 엔티티를 찾아서 설정
        if (requestDto.getParentCode() != null && !requestDto.getParentCode().isEmpty()) {
            AccountSubject parent = accountSubjectRepository.findByCode(requestDto.getParentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 계정을 찾을 수 없습니다. 코드: " + requestDto.getParentCode()));
            accountSubject.setParent(parent);
        }

        // SCD2 원칙에 따라 초기 유효기간 설정
        if (accountSubject.getValidFrom() == null) {
            accountSubject.setValidFrom(LocalDate.now());
        }
        if (accountSubject.getValidTo() == null) {
            accountSubject.setValidTo(LocalDate.of(9999, 12, 31));
        }
        return accountSubjectRepository.save(accountSubject);
    }

    /**
     * 코드로 특정 계정과목을 조회합니다.
     * @param code 조회할 계정 코드
     * @return Optional<AccountSubject>
     */
    @Transactional(readOnly = true)
    public Optional<AccountSubject> findAccountSubjectByCode(String code) {
        return accountSubjectRepository.findByCode(code);
    }

    /**
     * 현재 시점(today)에 유효한 모든 계정과목을 조회합니다.
     * @return 유효한 계정과목 리스트
     */
    @Transactional(readOnly = true)
    public List<AccountSubject> findAllActiveAccountSubjects() {
        LocalDate today = LocalDate.now();
        return accountSubjectRepository.findAll().stream()
                .filter(acc -> !today.isBefore(acc.getValidFrom()) && !today.isAfter(acc.getValidTo()))
                .collect(Collectors.toList());
    }

    /**
     * 계정과목 정보를 수정합니다.
     * @param code 수정할 계정 코드
     * @param requestDto 수정할 내용이 담긴 DTO
     * @return 수정된 계정과목 엔티티
     */
    public AccountSubject updateAccountSubject(String code, AccountSubjectRequestDto requestDto) {
        AccountSubject account = accountSubjectRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다. 코드: " + code));

        // 부모 계정 업데이트
        if (requestDto.getParentCode() != null && !requestDto.getParentCode().isEmpty()) {
            AccountSubject parent = accountSubjectRepository.findByCode(requestDto.getParentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 계정을 찾을 수 없습니다. 코드: " + requestDto.getParentCode()));
            account.setParent(parent);
        } else {
            account.setParent(null);
        }

        // 수정 가능한 필드 업데이트
        account.setName(requestDto.getName());
        account.setCategory(requestDto.getCategory());
        account.setBalanceType(requestDto.getBalanceType());
        account.setReportLine(requestDto.getReportLine());
        account.setUnsettled(requestDto.isUnsettled());
        account.setFixedAsset(requestDto.isFixedAsset());
        
        return accountSubjectRepository.save(account);
    }

    /**
     * 특정 계정과목을 비활성화합니다. (논리적 삭제)
     * @param code 비활성화할 계정 코드
     */
    public void deactivateAccountSubject(String code) {
        AccountSubject account = accountSubjectRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다. 코드: " + code));
        
        if (account.getValidTo().isAfter(LocalDate.now())) {
            account.setValidTo(LocalDate.now());
            accountSubjectRepository.save(account);
        }
    }
}

