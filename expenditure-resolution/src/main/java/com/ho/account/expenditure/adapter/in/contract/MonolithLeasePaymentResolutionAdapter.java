package com.ho.account.expenditure.adapter.in.contract;

import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import org.springframework.stereotype.Component;

@Component
public class MonolithLeasePaymentResolutionAdapter implements LeasePaymentResolutionPort {

    private final ExpenditureResolutionUseCase expenditureResolutionUseCase;

    public MonolithLeasePaymentResolutionAdapter(ExpenditureResolutionUseCase expenditureResolutionUseCase) {
        this.expenditureResolutionUseCase = expenditureResolutionUseCase;
    }

    @Override
    public void createLeasePaymentResolution(LeasePaymentResolutionCommand command) {
        // ID 기반 참조를 사용하므로 더 이상 타 모듈의 엔티티를 조회할 필요가 없음
        // resolution의 paymentAccountCode는 대변(Credit) 계정으로 사용됨
        ExpenditureResolution resolution = ExpenditureResolution.create(
                null, // resolutionNo는 서비스 레이어에서 생성
                command.title(),
                command.resolutionDate(),
                command.paymentDate(),
                command.departmentCode(),
                command.creditAccountCode(), // 대변(Credit) 계정: 예) 미지급금
                "SYSTEM_LEASE"
        );

        // detail의 accountCode는 차변(Debit) 계정으로 사용됨
        ExpenditureDetail detail = ExpenditureDetail.create(
                command.debitAccountCode(), // 차변(Debit) 계정: 예) 리스부채 또는 비용
                command.amount(),
                command.businessPartnerCode(),
                command.detailDescription()
        );
        resolution.addDetail(detail);

        expenditureResolutionUseCase.createResolution(resolution);
    }
}
