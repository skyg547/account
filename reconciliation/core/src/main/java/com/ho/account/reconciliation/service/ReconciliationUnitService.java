package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.application.port.in.ReconciliationUnitCommand;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [단일 책임 원칙 (SRP) - 대사 단위 관리 도메인 서비스]
 * 대사 단위({@link ReconciliationUnit})의 생성, 조회, 수정, 소프트 삭제(Soft Delete) 관리를 전담하는 전용 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명: Single Responsibility Principle (SRP) 적용]
 * 과거 거대 서비스({@link ReconciliationService})는 대사 단위 CRUD, 대사 규칙 CRUD, 대사 차액 배정/해결,
 * 그리고 실제 대사 실행(Run) 오케스트레이션까지 수많은 책임을 한 곳에 몰아서 처리하였습니다.
 * 이로 인해 클래스가 680행 이상으로 팽창하였고(God Class), 한 기능을 수정할 때 예상치 못한 다른 영역의 부작용이 발생할 위험이 컸습니다.
 * 
 * SRP(Single Responsibility Principle)에 따라 대사 단위 관리 기능을 이 클래스({@link ReconciliationUnitService})로 분리함으로서:
 * 1. **변경의 이유를 하나로 격리**: 대사 단위 관리 로직이 변경되더라도 대사 실행 알고리즘이나 규칙 관리에는 영향이 없습니다.
 * 2. **높은 응집도와 높은 가독성**: 대사 단위와 관련된 비즈니스 및 영속성 작업에만 집중하여 코드를 파악하기 쉽습니다.
 * 3. **안전한 데이터 보존 (Soft Delete)**: 대사 단위 삭제 요청 시 물리적 삭제 대신 inactive 처리(Soft Delete)하여
 *    과거 이력 추적성(Audit Trail) 및 데이터 무결성을 보장합니다.
 */
@Service
@Transactional
public class ReconciliationUnitService {

    private final ReconciliationUnitRepository reconciliationUnitRepository;

    public ReconciliationUnitService(ReconciliationUnitRepository reconciliationUnitRepository) {
        this.reconciliationUnitRepository = reconciliationUnitRepository;
    }

    /**
     * 새로운 대사 단위를 생성하고 저장합니다.
     *
     * @param command 대사 단위 생성에 필요한 속성을 담은 커맨드 객체
     * @return 저장된 대사 단위 엔티티
     */
    public ReconciliationUnit createReconciliationUnit(ReconciliationUnitCommand command) {
        ReconciliationUnit reconciliationUnit = toReconciliationUnit(command);
        return reconciliationUnitRepository.save(reconciliationUnit);
    }

    /**
     * 등록된 모든 대사 단위를 조회합니다.
     *
     * @return 대사 단위 전체 목록
     */
    @Transactional(readOnly = true)
    public List<ReconciliationUnit> findAllReconciliationUnits() {
        return reconciliationUnitRepository.findAll();
    }

    /**
     * 식별자(ID)로 대사 단위를 단건 조회합니다.
     *
     * @param id 대사 단위 ID
     * @return 조회된 대사 단위 엔티티
     * @throws EntityNotFoundException 해당 ID의 대사 단위가 없을 경우 발생
     */
    @Transactional(readOnly = true)
    public ReconciliationUnit findReconciliationUnitById(Long id) {
        return reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));
    }

    /**
     * 기존 대사 단위의 정보를 수정합니다.
     *
     * @param id 수정할 대사 단위 ID
     * @param command 변경할 대사 단위 속성 커맨드
     * @return 수정 및 저장된 대사 단위 엔티티
     * @throws EntityNotFoundException 대상 대사 단위가 존재하지 않을 경우 발생
     */
    public ReconciliationUnit updateReconciliationUnit(Long id, ReconciliationUnitCommand command) {
        ReconciliationUnit existingUnit = findReconciliationUnitById(id);

        existingUnit.setName(command.name());
        existingUnit.setDescription(command.description());
        existingUnit.setFrequency(command.frequency());
        existingUnit.setReconciliationType(command.reconciliationType());
        existingUnit.setCriteriaJson(command.criteriaJson());
        existingUnit.setActive(command.active());
        return reconciliationUnitRepository.save(existingUnit);
    }

    /**
     * 대사 단위를 비활성화(Soft Delete) 처리합니다.
     * 
     * [설계 이유]
     * 대사 단위는 과거 생성된 대사 실행 이력(ReconciliationRun) 및 차액(ReconciliationDifference)과
     * 관계를 맺고 있으므로 물리 삭제(Hard Delete)를 수행할 경우 과거 대사 데이터의 참조 무결성이 파괴됩니다.
     * 따라서 비활성화(active = false) 상태로 전환하여 이력을 안전하게 보관합니다.
     *
     * @param id 비활성화할 대사 단위 ID
     */
    public void deleteReconciliationUnit(Long id) {
        ReconciliationUnit existingUnit = findReconciliationUnitById(id);
        existingUnit.setActive(false);
        reconciliationUnitRepository.save(existingUnit);
    }

    /**
     * Command 객체를 엔티티 객체로 변환하는 private 헬퍼 메서드.
     */
    private ReconciliationUnit toReconciliationUnit(ReconciliationUnitCommand command) {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setName(command.name());
        unit.setDescription(command.description());
        unit.setFrequency(command.frequency());
        unit.setReconciliationType(command.reconciliationType());
        unit.setCriteriaJson(command.criteriaJson());
        unit.setActive(command.active());
        return unit;
    }
}
