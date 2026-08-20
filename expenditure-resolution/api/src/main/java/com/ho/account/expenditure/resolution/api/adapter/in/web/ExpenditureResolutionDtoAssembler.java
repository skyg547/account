package com.ho.account.expenditure.resolution.api.adapter.in.web;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.resolution.api.dto.ExpenditureResolutionDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ExpenditureResolutionDtoAssembler {

    private final MasterDataQueryPort masterDataQueryPort;

    public ExpenditureResolutionDtoAssembler(MasterDataQueryPort masterDataQueryPort) {
        this.masterDataQueryPort = masterDataQueryPort;
    }

    public ExpenditureResolutionDto toDto(ExpenditureResolution resolution) {
        List<ExpenditureResolutionDto.ExpenditureDetailDto> detailDtos = resolution.getDetails() == null
                ? List.of()
                : resolution.getDetails().stream()
                        .map(this::toDetailDto)
                        .toList();

        return new ExpenditureResolutionDto(
                resolution.getId(),
                resolution.getResolutionNo(),
                resolution.getTitle(),
                resolution.getResolutionDate(),
                resolution.getPaymentDate(),
                resolution.getDeptCode(),
                departmentName(resolution.getDeptCode()),
                resolution.getPaymentAccountCode(),
                accountSubjectName(resolution.getPaymentAccountCode()),
                resolution.getTotalAmount(),
                resolution.getStatus() != null ? resolution.getStatus().name() : null,
                resolution.getRejectionReason(),
                resolution.getTaxInvoiceId(),
                detailDtos);
    }

    public List<ExpenditureResolutionDto> toDtoList(List<ExpenditureResolution> resolutions) {
        return resolutions.stream()
                .map(this::toDto)
                .toList();
    }

    private ExpenditureResolutionDto.ExpenditureDetailDto toDetailDto(ExpenditureDetail detail) {
        return new ExpenditureResolutionDto.ExpenditureDetailDto(
                detail.getId(),
                detail.getAccountCode(),
                accountSubjectName(detail.getAccountCode()),
                detail.getAmount(),
                detail.getBusinessPartnerCode(),
                businessPartnerName(detail.getBusinessPartnerCode()),
                detail.getDescription());
    }

    private String departmentName(String code) {
        return Optional.ofNullable(code)
                .filter(c -> !c.isBlank())
                .flatMap(masterDataQueryPort::findDepartment)
                .map(DepartmentRef::name)
                .orElse(null);
    }

    private String accountSubjectName(String code) {
        return Optional.ofNullable(code)
                .filter(c -> !c.isBlank())
                .flatMap(masterDataQueryPort::findAccountSubject)
                .map(AccountSubjectRef::name)
                .orElse(null);
    }

    private String businessPartnerName(String code) {
        return Optional.ofNullable(code)
                .filter(c -> !c.isBlank())
                .flatMap(masterDataQueryPort::findBusinessPartner)
                .map(BusinessPartnerRef::name)
                .orElse(null);
    }
}