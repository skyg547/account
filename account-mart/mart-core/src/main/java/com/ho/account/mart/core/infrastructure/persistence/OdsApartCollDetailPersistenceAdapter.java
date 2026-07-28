package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsApartCollDetailRepository;
import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsApartCollDetailEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsApartCollDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * [Persistence Adapter] 아파트 담보 상세 헥사고날 어댑터.
 *
 * <p>초보자 설명: application port의 요청을 JPA Entity 저장/조회로 바꾸는 변환 계층입니다.
 * domain 객체와 DB Entity를 분리하면 LGD/DQ 업무 규칙이 DB 컬럼명 변경에 직접 흔들리지 않습니다.</p>
 */
@Component
@RequiredArgsConstructor
public class OdsApartCollDetailPersistenceAdapter implements OdsApartCollDetailRepository {

    private final JpaOdsApartCollDetailRepository jpaRepository;

    @Override
    public Optional<OdsApartCollDetail> findByCollateralId(String collateralId) {
        if (collateralId == null || collateralId.isBlank()) {
            return Optional.empty();
        }
        return jpaRepository.findById(collateralId).map(this::main);
    }

    @Override
    public OdsApartCollDetail save(OdsApartCollDetail detail) {
        return main(jpaRepository.save(toEntity(detail)));
    }

    @Override
    public void saveAll(Iterable<OdsApartCollDetail> details) {
        if (details == null) {
            return;
        }
        List<OdsApartCollDetailEntity> entities = StreamSupport.stream(details.spliterator(), false)
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    private OdsApartCollDetail main(OdsApartCollDetailEntity entity) {
        if (entity == null) {
            return null;
        }
        return OdsApartCollDetail.builder()
                .collateralId(entity.getCollateralId())
                .districtCode(entity.getDistrictCode())
                .kbMarketPrice(entity.getKbMarketPrice())
                .houseType(entity.getHouseType())
                .exclusiveArea(entity.getExclusiveArea())
                .floorNo(entity.getFloorNo())
                .isSpeculativeArea(entity.getIsSpeculativeArea())
                .build();
    }

    private OdsApartCollDetailEntity toEntity(OdsApartCollDetail detail) {
        if (detail == null) {
            return null;
        }
        return OdsApartCollDetailEntity.builder()
                .collateralId(detail.getCollateralId())
                .districtCode(detail.getDistrictCode())
                .kbMarketPrice(detail.getKbMarketPrice())
                .houseType(detail.getHouseType())
                .exclusiveArea(detail.getExclusiveArea())
                .floorNo(detail.getFloorNo())
                .isSpeculativeArea(Boolean.TRUE.equals(detail.getIsSpeculativeArea()))
                .build();
    }
}
