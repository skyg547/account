package com.ho.account.expenditure.resolution.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.resolution.api.dto.ExpenditureResolutionDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExpenditureResolutionDtoAssemblerTest {

    private MasterDataQueryPort masterDataQueryPort;
    private ExpenditureResolutionDtoAssembler assembler;

    @BeforeEach
    void setUp() {
        masterDataQueryPort = mock(MasterDataQueryPort.class);
        assembler = new ExpenditureResolutionDtoAssembler(masterDataQueryPort);
    }

    @Test
    @DisplayName("지출결의서 도메인 모델을 DTO로 안전하게 변환하고 기준정보 명칭을 매핑한다")
    void toDtoMapsEntityWithMasterDataNames() {
        when(masterDataQueryPort.findDepartment("DEPT-100"))
                .thenReturn(Optional.of(new DepartmentRef("DEPT-100", "재무팀", "DEPARTMENT")));
        when(masterDataQueryPort.findAccountSubject("ACC-101"))
                .thenReturn(Optional.of(new AccountSubjectRef("ACC-101", "보통예금", false, false, "DEBIT")));
        when(masterDataQueryPort.findAccountSubject("ACC-501"))
                .thenReturn(Optional.of(new AccountSubjectRef("ACC-501", "지급수수료", false, false, "DEBIT")));
        when(masterDataQueryPort.findBusinessPartner("BP-200"))
                .thenReturn(Optional.of(new BusinessPartnerRef("BP-200", "주식회사 한국벤더", "VENDOR", true)));

        ExpenditureResolution resolution = ExpenditureResolution.create(
                "EXP-202608-001",
                "8월 클라우드 인프라 사용료 지출결의",
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 25),
                "DEPT-100",
                "ACC-101",
                "user_kim"
        );
        resolution.setTaxInvoiceId(999L);
        resolution.addDetail(ExpenditureDetail.create(
                "ACC-501",
                new BigDecimal("5500000.00"),
                "BP-200",
                "AWS 클라우드 사용료 결제"
        ));

        ExpenditureResolutionDto dto = assembler.toDto(resolution);

        assertThat(dto).isNotNull();
        assertThat(dto.getResolutionNo()).isEqualTo("EXP-202608-001");
        assertThat(dto.getDepartmentName()).isEqualTo("재무팀");
        assertThat(dto.getPaymentAccountName()).isEqualTo("보통예금");
        assertThat(dto.getDetails()).hasSize(1);
        assertThat(dto.getDetails().get(0).getAccountSubjectName()).isEqualTo("지급수수료");
        assertThat(dto.getDetails().get(0).getBusinessPartnerName()).isEqualTo("주식회사 한국벤더");
    }

    @Test
    @DisplayName("기준정보 코드가 null 또는 미등록인 경우 예외 없이 안전하게 null을 반환한다")
    void toDtoHandlesNullAndUnmatchedMasterDataGracefully() {
        when(masterDataQueryPort.findDepartment("UNKNOWN_DEPT")).thenReturn(Optional.empty());

        ExpenditureResolution resolution = ExpenditureResolution.create(
                "EXP-202608-002",
                "기타 지출",
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 25),
                null,
                null,
                "user_lee"
        );

        ExpenditureResolutionDto dto = assembler.toDto(resolution);

        assertThat(dto).isNotNull();
        assertThat(dto.getDepartmentName()).isNull();
        assertThat(dto.getPaymentAccountName()).isNull();
        assertThat(dto.getDetails()).isEmpty();
    }
}
