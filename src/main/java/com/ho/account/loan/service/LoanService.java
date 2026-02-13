package com.ho.account.loan.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import com.ho.account.loan.domain.LoanContract;
import com.ho.account.loan.dto.LoanContractRequestDto;
import com.ho.account.loan.repository.LoanAmortizationScheduleEntryRepository;
import com.ho.account.loan.repository.LoanContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ho.account.audit.domain.AuditLoggable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LoanService {

    private final LoanContractRepository loanContractRepository;
    private final LoanAmortizationScheduleEntryRepository amortizationRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final EIRCalculator eirCalculator;

    @Autowired
    public LoanService(LoanContractRepository loanContractRepository,
            LoanAmortizationScheduleEntryRepository amortizationRepository,
            BusinessPartnerRepository businessPartnerRepository,
            EIRCalculator eirCalculator) {
        this.loanContractRepository = loanContractRepository;
        this.amortizationRepository = amortizationRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.eirCalculator = eirCalculator;
    }

    /**
     * 새로운 대출 계약을 생성하고 EIR을 계산하여 상각 스케줄을 생성합니다.
     * 
     * @param requestDto 생성할 대출 계약 정보가 담긴 DTO
     * @return 생성된 LoanContract 엔티티
     */
    @AuditLoggable(eventType = "LOAN", eventName = "CREATE_CONTRACT")
    public LoanContract createLoanContract(LoanContractRequestDto requestDto) {
        if (loanContractRepository.existsByLoanContractNo(requestDto.getLoanContractNo())) {
            throw new IllegalArgumentException("이미 존재하는 대출 계약 번호입니다: " + requestDto.getLoanContractNo());
        }

        BusinessPartner businessPartner = businessPartnerRepository
                .findByBusinessPartnerCode(requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "거래처를 찾을 수 없습니다. 코드: " + requestDto.getBusinessPartnerCode()));

        LoanContract loanContract = new LoanContract();
        loanContract.setLoanContractNo(requestDto.getLoanContractNo());
        loanContract.setBusinessPartner(businessPartner);
        loanContract.setLoanProduct(requestDto.getLoanProduct());
        loanContract.setPrincipalAmount(requestDto.getPrincipalAmount());
        loanContract.setCurrentPrincipalBalance(requestDto.getPrincipalAmount()); // 초기 원금 잔액은 대출 원금과 동일
        loanContract.setDisbursementDate(requestDto.getDisbursementDate());
        loanContract.setMaturityDate(requestDto.getMaturityDate());
        loanContract.setInterestRate(requestDto.getInterestRate());
        loanContract.setRepaymentMethod(requestDto.getRepaymentMethod());
        loanContract.setStatus(requestDto.getStatus() != null ? requestDto.getStatus() : "ACTIVE");
        loanContract.setDeferredLoanFee(requestDto.getDeferredLoanFee());

        // EIR 계산 (단순화된 예시, 실제 구현은 더 복잡할 수 있음)
        BigDecimal effectiveInterestRate = calculateEffectiveInterestRate(loanContract);
        loanContract.setEffectiveInterestRate(effectiveInterestRate);

        LoanContract savedContract = loanContractRepository.save(loanContract);

        // 상각 스케줄 생성
        generateAmortizationSchedule(savedContract);

        return savedContract;
    }

    /**
     * ID로 대출 계약을 조회합니다.
     * 
     * @param id 조회할 대출 계약 ID
     * @return Optional<LoanContract>
     */
    @Transactional(readOnly = true)
    public Optional<LoanContract> getLoanContractById(Long id) {
        return loanContractRepository.findById(id);
    }

    /**
     * 대출 계약 번호로 대출 계약을 조회합니다.
     * 
     * @param loanContractNo 조회할 대출 계약 번호
     * @return Optional<LoanContract>
     */
    @Transactional(readOnly = true)
    public Optional<LoanContract> getLoanContractByLoanContractNo(String loanContractNo) {
        return loanContractRepository.findByLoanContractNo(loanContractNo);
    }

    /**
     * 모든 대출 계약을 조회합니다.
     * 
     * @return 대출 계약 리스트
     */
    @Transactional(readOnly = true)
    public List<LoanContract> getAllLoanContracts() {
        return loanContractRepository.findAll();
    }

    /**
     * 대출 계약 정보를 수정합니다.
     * 
     * @param id         수정할 대출 계약 ID
     * @param requestDto 수정할 내용이 담긴 DTO
     * @return 수정된 LoanContract 엔티티
     */
    @AuditLoggable(eventType = "LOAN", eventName = "UPDATE_CONTRACT")
    public LoanContract updateLoanContract(Long id, LoanContractRequestDto requestDto) {
        LoanContract existingContract = loanContractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대출 계약을 찾을 수 없습니다. ID: " + id));

        // 대출 계약 번호 변경 시 중복 확인
        if (!existingContract.getLoanContractNo().equals(requestDto.getLoanContractNo())
                && loanContractRepository.existsByLoanContractNo(requestDto.getLoanContractNo())) {
            throw new IllegalArgumentException("이미 존재하는 대출 계약 번호입니다: " + requestDto.getLoanContractNo());
        }

        BusinessPartner businessPartner = businessPartnerRepository
                .findByBusinessPartnerCode(requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "거래처를 찾을 수 없습니다. 코드: " + requestDto.getBusinessPartnerCode()));

        existingContract.setLoanContractNo(requestDto.getLoanContractNo());
        existingContract.setBusinessPartner(businessPartner);
        existingContract.setLoanProduct(requestDto.getLoanProduct());
        existingContract.setPrincipalAmount(requestDto.getPrincipalAmount());
        existingContract.setDisbursementDate(requestDto.getDisbursementDate());
        existingContract.setMaturityDate(requestDto.getMaturityDate());
        existingContract.setInterestRate(requestDto.getInterestRate());
        existingContract.setRepaymentMethod(requestDto.getRepaymentMethod());
        existingContract
                .setStatus(requestDto.getStatus() != null ? requestDto.getStatus() : existingContract.getStatus());
        existingContract.setDeferredLoanFee(requestDto.getDeferredLoanFee());

        // EIR 재계산 (필요 시)
        BigDecimal effectiveInterestRate = calculateEffectiveInterestRate(existingContract);
        existingContract.setEffectiveInterestRate(effectiveInterestRate);

        // 상각 스케줄 재-생성 (대출 금액, 이자율, 기간 등 변경 시)
        // TODO: 기존 스케줄 삭제 후 새로 생성하거나, 변경된 부분만 수정하는 로직 필요
        amortizationRepository.findByLoanContractIdOrderByPeriodNumberAsc(existingContract.getId())
                .forEach(amortizationRepository::delete);
        generateAmortizationSchedule(existingContract);

        return loanContractRepository.save(existingContract);
    }

    /**
     * 대출 계약을 삭제합니다.
     * 
     * @param id 삭제할 대출 계약 ID
     */
    @AuditLoggable(eventType = "LOAN", eventName = "DELETE_CONTRACT")
    public void deleteLoanContract(Long id) {
        LoanContract loanContract = loanContractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대출 계약을 찾을 수 없습니다. ID: " + id));
        // 연관된 상각 스케줄 항목들도 함께 삭제 (orphanRemoval = true 설정으로 cascade 됨)
        loanContractRepository.delete(loanContract);
    }

    /**
     * EIR (Effective Interest Rate)을 계산합니다.
     * 이 메서드는 실제 EIR 계산 로직을 포함해야 합니다.
     * 여기서는 단순화를 위해 명목 이자율을 반환합니다.
     * 실제 EIR 계산은 Newton-Raphson method 등 복잡한 수치 해석 방법을 사용해야 합니다.
     *
     * @param loanContract 대출 계약 엔티티
     * @return 계산된 유효 이자율
     */
    private BigDecimal calculateEffectiveInterestRate(LoanContract loanContract) {
        long totalMonths = ChronoUnit.MONTHS.between(loanContract.getDisbursementDate(),
                loanContract.getMaturityDate());
        if (totalMonths <= 0) {
            return loanContract.getInterestRate();
        }
        return eirCalculator.calculateEIR(loanContract, (int) totalMonths);
    }

    /**
     * 대출 상각 스케줄을 생성합니다.
     *
     * @param loanContract 대출 계약 엔티티
     */
    private void generateAmortizationSchedule(LoanContract loanContract) {
        // TODO: 실제 상각 스케줄 생성 로직 구현
        // 예시: 원리금 균등 상환 방식
        if ("원리금균등".equals(loanContract.getRepaymentMethod())) {
            BigDecimal principal = loanContract.getPrincipalAmount();
            BigDecimal annualInterestRate = loanContract.getEffectiveInterestRate();
            LocalDate disbursementDate = loanContract.getDisbursementDate();
            LocalDate maturityDate = loanContract.getMaturityDate();

            long totalMonths = ChronoUnit.MONTHS.between(disbursementDate, maturityDate);
            if (totalMonths <= 0) {
                throw new IllegalArgumentException("만기일이 실행일보다 빠르거나 같습니다.");
            }

            // 월 이자율
            BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10,
                    RoundingMode.HALF_UP); // 12개월, %

            // 월 상환액 (PMT 공식)
            BigDecimal pmtNumerator = monthlyInterestRate.multiply(principal);
            BigDecimal pmtDenominator = BigDecimal.ONE
                    .subtract(BigDecimal.ONE.add(monthlyInterestRate).pow(Math.negateExact((int) totalMonths)));
            BigDecimal monthlyPayment = pmtNumerator.divide(pmtDenominator, 2, RoundingMode.HALF_UP);

            BigDecimal outstandingBalance = principal;
            LocalDate currentPaymentDate = disbursementDate;

            for (int i = 1; i <= totalMonths; i++) {
                currentPaymentDate = currentPaymentDate.plusMonths(1); // 매월 1일로 가정

                BigDecimal interestPayment = outstandingBalance.multiply(monthlyInterestRate).setScale(2,
                        RoundingMode.HALF_UP);
                BigDecimal principalPayment = monthlyPayment.subtract(interestPayment);

                // 마지막 회차 조정
                if (i == totalMonths) {
                    principalPayment = outstandingBalance; // 마지막 회차 원금은 남은 잔액
                    monthlyPayment = outstandingBalance.add(interestPayment);
                }

                BigDecimal endingBalance = outstandingBalance.subtract(principalPayment);
                if (endingBalance.compareTo(BigDecimal.ZERO) < 0) {
                    endingBalance = BigDecimal.ZERO; // 잔액이 음수가 되는 경우 0으로 조정
                }

                LoanAmortizationScheduleEntry entry = new LoanAmortizationScheduleEntry();
                entry.setLoanContract(loanContract);
                entry.setPaymentDate(currentPaymentDate);
                entry.setPeriodNumber(i);
                entry.setStartingBalance(outstandingBalance);
                entry.setScheduledPaymentAmount(monthlyPayment);
                entry.setInterestAmount(interestPayment);
                entry.setPrincipalAmount(principalPayment);
                entry.setEndingBalance(endingBalance);
                entry.setDeferredFeeAmortization(BigDecimal.ZERO); // TODO: EIR 상각액 계산 로직 추가
                entry.setEntryType("REPAYMENT");

                loanContract.addAmortizationEntry(entry); // 연관관계 편의 메서드 사용
                amortizationRepository.save(entry);

                outstandingBalance = endingBalance;
            }
        } else {
            // TODO: 다른 상환 방식 구현 (만기일시 상환 등)
            // 현재는 원리금 균등만 지원
        }
    }

    /**
     * 대출 상환을 처리합니다.
     * 
     * @param loanContractId 대출 계약 ID
     * @param paymentAmount  실제 상환 금액
     * @param paymentDate    실제 상환일
     */
    public void processLoanRepayment(Long loanContractId, BigDecimal paymentAmount, LocalDate paymentDate) {
        LoanContract loanContract = loanContractRepository.findById(loanContractId)
                .orElseThrow(() -> new IllegalArgumentException("대출 계약을 찾을 수 없습니다. ID: " + loanContractId));

        if (loanContract.getCurrentPrincipalBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("이미 상환 완료된 대출입니다.");
        }

        // TODO: 실제 상환 처리 로직 구현 (현재 원금 잔액 업데이트, 상환 스케줄 조정 등)
        // 여기서는 단순히 현재 원금 잔액을 줄이는 예시
        loanContract.setCurrentPrincipalBalance(loanContract.getCurrentPrincipalBalance().subtract(paymentAmount));
        if (loanContract.getCurrentPrincipalBalance().compareTo(BigDecimal.ZERO) < 0) {
            loanContract.setCurrentPrincipalBalance(BigDecimal.ZERO);
        }

        // 상환 완료 여부 확인
        if (loanContract.getCurrentPrincipalBalance().compareTo(BigDecimal.ZERO) == 0) {
            loanContract.setStatus("PAID_OFF");
        }
        loanContractRepository.save(loanContract);

        // TODO: 전표 생성 로직 추가
    }
}
