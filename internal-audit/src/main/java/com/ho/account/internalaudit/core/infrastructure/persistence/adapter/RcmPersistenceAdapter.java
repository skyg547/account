package com.ho.account.internalaudit.core.infrastructure.persistence.adapter;

import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.ControlActivityJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.RcmProcessJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.RcmRiskJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.ControlActivityRepository;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.RcmProcessRepository;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.RcmRiskRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RcmPersistenceAdapter implements RcmPersistencePort {

    private final RcmProcessRepository processRepository;
    private final RcmRiskRepository riskRepository;
    private final ControlActivityRepository controlActivityRepository;

    @Override
    public RcmProcess saveProcess(RcmProcess process) {
        RcmProcessJpaEntity entity = RcmProcessJpaEntity.builder()
                .processId(process.processId())
                .processName(process.processName())
                .description(process.description())
                .ownerId(process.ownerId())
                .build();
        RcmProcessJpaEntity saved = processRepository.save(entity);
        return main(saved);
    }

    @Override
    public RcmRisk saveRisk(RcmRisk risk) {
        RcmRiskJpaEntity entity = RcmRiskJpaEntity.builder()
                .riskId(risk.riskId())
                .processId(risk.processId())
                .riskDescription(risk.riskDescription())
                .impactLevel(risk.impactLevel())
                .likelihood(risk.likelihood())
                .build();
        RcmRiskJpaEntity saved = riskRepository.save(entity);
        return main(saved);
    }

    @Override
    public ControlActivity saveControlActivity(ControlActivity controlActivity) {
        ControlActivityJpaEntity entity = ControlActivityJpaEntity.builder()
                .controlId(controlActivity.controlId())
                .riskId(controlActivity.riskId())
                .controlDescription(controlActivity.controlDescription())
                .controlType(controlActivity.controlType())
                .executionMethod(controlActivity.executionMethod())
                .frequency(controlActivity.frequency())
                .ownerId(controlActivity.ownerId())
                .build();
        ControlActivityJpaEntity saved = controlActivityRepository.save(entity);
        return main(saved);
    }

    @Override
    public Optional<RcmProcess> findProcessById(String processId) {
        return processRepository.findById(processId).map(this::main);
    }

    @Override
    public Optional<RcmRisk> findRiskById(String riskId) {
        return riskRepository.findById(riskId).map(this::main);
    }

    @Override
    public Optional<ControlActivity> findControlActivityById(String controlId) {
        return controlActivityRepository.findById(controlId).map(this::main);
    }

    @Override
    public List<RcmProcess> findAllProcesses() {
        return processRepository.findAll().stream().map(this::main).collect(Collectors.toList());
    }

    @Override
    public List<RcmRisk> findRisksByProcessId(String processId) {
        return riskRepository.findByProcessId(processId).stream().map(this::main).collect(Collectors.toList());
    }

    @Override
    public List<ControlActivity> findControlActivitiesByRiskId(String riskId) {
        return controlActivityRepository.findByRiskId(riskId).stream().map(this::main).collect(Collectors.toList());
    }

    private RcmProcess main(RcmProcessJpaEntity entity) {
        return RcmProcess.builder()
                .processId(entity.getProcessId())
                .processName(entity.getProcessName())
                .description(entity.getDescription())
                .ownerId(entity.getOwnerId())
                .build();
    }

    private RcmRisk main(RcmRiskJpaEntity entity) {
        return RcmRisk.builder()
                .riskId(entity.getRiskId())
                .processId(entity.getProcessId())
                .riskDescription(entity.getRiskDescription())
                .impactLevel(entity.getImpactLevel())
                .likelihood(entity.getLikelihood())
                .build();
    }

    private ControlActivity main(ControlActivityJpaEntity entity) {
        return ControlActivity.builder()
                .controlId(entity.getControlId())
                .riskId(entity.getRiskId())
                .controlDescription(entity.getControlDescription())
                .controlType(entity.getControlType())
                .executionMethod(entity.getExecutionMethod())
                .frequency(entity.getFrequency())
                .ownerId(entity.getOwnerId())
                .build();
    }
}
