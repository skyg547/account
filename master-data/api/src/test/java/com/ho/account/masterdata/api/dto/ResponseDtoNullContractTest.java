package com.ho.account.masterdata.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import org.junit.jupiter.api.Test;

/**
 * 응답 DTO 팩토리가 필수 인자에 대해 null을 조용히 흘려보내지 않는지 검증한다.
 *
 * <p>이전 구현은 {@code entity}가 null이면 null을 반환했다. 호출부는 그 결과를 그대로
 * {@code ResponseEntity.ok(...)}에 넣으므로, 그 경로가 도달했다면 클라이언트는
 * <strong>200 OK에 빈 본문</strong>을 받았을 것이다. 오류가 성공으로 보이는 형태다.</p>
 *
 * <p>현재 호출부는 모두 use case가 만든 엔티티이거나 {@code Optional.map} 내부라 null이
 * 올 수 없다. 이 테스트는 그 계약이 나중에 되돌아가지 않도록 고정한다.</p>
 */
class ResponseDtoNullContractTest {

    @Test
    void accountSubjectFactoryRejectsNullEntity() {
        assertThatThrownBy(() -> AccountSubjectDto.fromEntity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("AccountSubject");
    }

    @Test
    void departmentFactoryRejectsNullEntity() {
        assertThatThrownBy(() -> DepartmentDto.fromEntity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Department");
    }

    @Test
    void accountSubjectFactoryMapsEntityWithoutParent() {
        AccountSubject entity = new AccountSubject();
        entity.setCode("101010");
        entity.setName("현금");

        AccountSubjectDto dto = AccountSubjectDto.fromEntity(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getCode()).isEqualTo("101010");
        assertThat(dto.getName()).isEqualTo("현금");
        // parent가 없는 것은 정상이며, 그 경우 parentCode는 null로 남는다.
        assertThat(dto.getParentCode()).isNull();
    }

    @Test
    void departmentFactoryMapsEntityWithoutParent() {
        Department entity = new Department();
        entity.setCode("D100");
        entity.setName("회계팀");

        DepartmentDto dto = DepartmentDto.fromEntity(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getCode()).isEqualTo("D100");
        assertThat(dto.getName()).isEqualTo("회계팀");
        assertThat(dto.getParentCode()).isNull();
    }
}
