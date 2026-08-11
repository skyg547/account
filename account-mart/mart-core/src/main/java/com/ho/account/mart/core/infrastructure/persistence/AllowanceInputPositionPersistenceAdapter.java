package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.application.port.out.AllowanceInputPositionRepository;
import com.ho.account.mart.core.infrastructure.persistence.entity.mart.AllowanceInputPositionEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaAllowanceInputPositionRepository;
import com.ho.account.mart.core.infrastructure.persistence.mapper.AllowanceInputPositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * [Adapter] AllowanceInputPositionRepository 포트의 JPA 구현체.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * 헥사고날 아키텍처에서 이 클래스는 출력 어댑터(Outbound Adapter) 역할을 합니다.
 * 애플리케이션 핵심 포트({@link AllowanceInputPositionRepository})의 계약에 따라 도메인 객체({@link AllowanceInputPosition})를 주고받고,
 * 내부적으로 JPA 영속성 엔티티({@link AllowanceInputPositionEntity}) 및 매퍼({@link AllowanceInputPositionMapper})를 통해 DB 연동을 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class AllowanceInputPositionPersistenceAdapter implements AllowanceInputPositionRepository {

    private final JpaAllowanceInputPositionRepository jpaRepository;

    @Override
    public void deleteByBaseDt(LocalDate baseDt) {
        jpaRepository.deleteByBaseDt(baseDt);
    }

    @Override
    public Page<AllowanceInputPosition> findByBaseDt(LocalDate baseDt, Pageable pageable) {
        return jpaRepository.findByBaseDt(baseDt, pageable)
                .map(AllowanceInputPositionMapper::toDomain);
    }

    @Override
    public List<AllowanceInputPosition> findByBaseDt(LocalDate baseDt) {
        return jpaRepository.findByBaseDt(baseDt).stream()
                .map(AllowanceInputPositionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductCurrencyBalanceSummary> getBalanceSummaryByBaseDate(LocalDate baseDt) {
        return jpaRepository.getBalanceSummaryByBaseDate(baseDt);
    }

    @Override
    public List<StagingDistribution> getStagingDistribution(LocalDate baseDt) {
        return jpaRepository.getStagingDistribution(baseDt);
    }

    @Override
    public List<SectorDistribution> getSectorDistribution(LocalDate baseDt) {
        return jpaRepository.getSectorDistribution(baseDt);
    }

    @Override
    public Map<String, Object> getSummaryMetrics(LocalDate baseDt) {
        return jpaRepository.getSummaryMetrics(baseDt);
    }

    @Override
    public AllowanceInputPosition save(AllowanceInputPosition position) {
        AllowanceInputPositionEntity entity = AllowanceInputPositionMapper.toEntity(position);
        AllowanceInputPositionEntity savedEntity = jpaRepository.save(entity);
        return AllowanceInputPositionMapper.toDomain(savedEntity);
    }

    @Override
    public void saveAll(Iterable<AllowanceInputPosition> positions) {
        List<AllowanceInputPositionEntity> entities = StreamSupport.stream(positions.spliterator(), false)
                .map(AllowanceInputPositionMapper::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }
}


