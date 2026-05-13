package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentPersistencePort departmentPersistencePort;

    private DepartmentService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(departmentPersistencePort);
    }

    @Test
    void updateDepartmentCreatesNewScd2VersionAndClosesCurrentVersion() {
        Department parent = department(1L, "ROOT", "Root", Department.DepartmentType.SUPPORT,
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31));
        Department current = department(10L, "D001", "Old department", Department.DepartmentType.COST_CENTER,
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31));
        current.setParent(parent);
        DepartmentCommand command = new DepartmentCommand(
                "D001",
                "New department",
                null,
                null,
                LocalDate.of(2026, 7, 1),
                null
        );

        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(current));
        when(departmentPersistencePort.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department updated = service.updateDepartment("D001", command);

        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentPersistencePort, times(2)).save(departmentCaptor.capture());

        assertThat(current.getValidTo()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(departmentCaptor.getAllValues().get(0)).isSameAs(current);
        assertThat(departmentCaptor.getAllValues().get(1)).isSameAs(updated);
        assertThat(updated).isNotSameAs(current);
        assertThat(updated.getId()).isNull();
        assertThat(updated.getCode()).isEqualTo("D001");
        assertThat(updated.getName()).isEqualTo("New department");
        assertThat(updated.getType()).isEqualTo(Department.DepartmentType.COST_CENTER);
        assertThat(updated.getParent()).isSameAs(parent);
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
        assertThat(departmentCaptor.getAllValues()).hasSize(2);
    }

    @Test
    void updateDepartmentUsesTodayWhenValidFromIsMissing() {
        Department current = department(10L, "D001", "Old department", Department.DepartmentType.COST_CENTER,
                LocalDate.now().minusDays(10), LocalDate.of(9999, 12, 31));
        DepartmentCommand command = new DepartmentCommand(
                "D001",
                "Today version",
                null,
                Department.DepartmentType.PROFIT_CENTER,
                null,
                null
        );

        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(current));
        when(departmentPersistencePort.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department updated = service.updateDepartment("D001", command);

        assertThat(current.getValidTo()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.now());
        assertThat(updated.getType()).isEqualTo(Department.DepartmentType.PROFIT_CENTER);
    }

    @Test
    void updateDepartmentRejectsCodeChange() {
        Department current = department(10L, "D001", "Old department", Department.DepartmentType.COST_CENTER,
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31));
        DepartmentCommand command = new DepartmentCommand(
                "D002",
                "New department",
                null,
                null,
                LocalDate.of(2026, 7, 1),
                null
        );

        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.updateDepartment("D001", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("부서 코드는");
    }

    private Department department(Long id, String code, String name, Department.DepartmentType type,
            LocalDate validFrom, LocalDate validTo) {
        Department department = new Department();
        department.setId(id);
        department.setCode(code);
        department.setName(name);
        department.setType(type);
        department.setValidFrom(validFrom);
        department.setValidTo(validTo);
        return department;
    }
}
