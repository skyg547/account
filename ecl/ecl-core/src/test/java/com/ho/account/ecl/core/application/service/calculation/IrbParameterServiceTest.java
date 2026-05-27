package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrRegulatoryParameterRepository;
import com.ho.account.ecl.core.domain.model.CrRegulatoryParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IrbParameterServiceTest {

    @Mock
    private CrRegulatoryParameterRepository parameterRepository;

    @InjectMocks
    private IrbParameterService irbParameterService;

    @Test
    @DisplayName("✅ 필수 파라미터가 모두 존재할 경우 정상적으로 로드되어야 함")
    void should_LoadParams_When_AllRequiredExist() {
        // given
        given(parameterRepository.findAll()).willReturn(List.of(
                new CrRegulatoryParameter("PD_FLOOR", new BigDecimal("0.0005"), "PD 하한선", null),
                new CrRegulatoryParameter("SECURED_LGD_FLOOR", new BigDecimal("0.20"), "담보부 LGD 하한선", null),
                new CrRegulatoryParameter("UNSECURED_LGD_FLOOR", new BigDecimal("0.45"), "무담보부 LGD 하한선", null)
        ));

        // when
        irbParameterService.refreshCache();

        // then
        assertThat(irbParameterService.getParameters().getPdFloor()).isEqualTo(new BigDecimal("0.0005"));
        assertThat(irbParameterService.getParameters().getUnsecuredLgdFloor()).isEqualTo(new BigDecimal("0.45"));
    }

    @Test
    @DisplayName("❌ 필수 파라미터(PD_FLOOR) 누락 시 IllegalStateException 발생해야 함")
    void should_ThrowException_When_RequiredParamMissing() {
        // given
        given(parameterRepository.findAll()).willReturn(List.of(
                new CrRegulatoryParameter("SECURED_LGD_FLOOR", new BigDecimal("0.20"), "담보부 LGD 하한선", null),
                new CrRegulatoryParameter("UNSECURED_LGD_FLOOR", new BigDecimal("0.45"), "무담보부 LGD 하한선", null)
        ));

        // when & then
        assertThatThrownBy(() -> irbParameterService.refreshCache())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("필수 규제 파라미터 누락: PD_FLOOR");
    }

    @Test
    @DisplayName("❌ 파라미터 값이 0 이하일 경우 IllegalArgumentException 발생해야 함")
    void should_ThrowException_When_ParamValueIsInvalid() {
        // given
        given(parameterRepository.findAll()).willReturn(List.of(
                new CrRegulatoryParameter("PD_FLOOR", new BigDecimal("0.0000"), "잘못된 PD 하한선", null),
                new CrRegulatoryParameter("SECURED_LGD_FLOOR", new BigDecimal("0.20"), "담보부 LGD 하한선", null),
                new CrRegulatoryParameter("UNSECURED_LGD_FLOOR", new BigDecimal("0.45"), "무담보부 LGD 하한선", null)
        ));

        // when & then
        assertThatThrownBy(() -> irbParameterService.refreshCache())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 규제 파라미터 값: PD_FLOOR");
    }
}
