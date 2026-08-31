package com.ho.account.receivable.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.CollectionMapper;
import com.ho.account.receivable.repository.CollectionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionPersistenceAdapterTest {

    @Mock
    private CollectionRepository collectionRepository;

    private CollectionMapper collectionMapper;
    private CollectionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        collectionMapper = new CollectionMapper();
        adapter = new CollectionPersistenceAdapter(collectionRepository, collectionMapper);
    }

    @Test
    @DisplayName("수납을 저장하고 도메인 모델로 변환하여 반환한다")
    void savesCollectionSuccessfully() {
        Collection domain = new Collection();
        domain.setCollectionDate(LocalDate.of(2026, 5, 29));
        domain.setCustomerCode("C001");
        domain.setAmount(new BigDecimal("100.00"));
        domain.setBankAccount("BANK-001");
        domain.setVirtualAccount("VA-001");
        domain.setReferenceNo("REF-PERSIST-100");
        domain.setStatus(CollectionStatus.RECEIVED);

        CollectionJpaEntity savedEntity = collectionMapper.toEntity(domain);
        savedEntity.setId(10L);

        when(collectionRepository.save(org.mockito.ArgumentMatchers.any(CollectionJpaEntity.class)))
                .thenReturn(savedEntity);

        Collection result = adapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getReferenceNo()).isEqualTo("REF-PERSIST-100");
        assertThat(result.getCustomerCode()).isEqualTo("C001");
    }

    @Test
    @DisplayName("존재하는 referenceNo 조회 시 도메인 객체로 변환하여 반환한다")
    void findsByReferenceNoSuccessfully() {
        CollectionJpaEntity entity = new CollectionJpaEntity();
        entity.setId(10L);
        entity.setReferenceNo("REF-001");
        entity.setCustomerCode("C001");
        entity.setAmount(new BigDecimal("200.00"));
        entity.setMatchedAmount(BigDecimal.ZERO);
        entity.setCollectionDate(LocalDate.of(2026, 5, 29));
        entity.setStatus(CollectionStatus.RECEIVED);

        when(collectionRepository.findByReferenceNo("REF-001")).thenReturn(Optional.of(entity));

        Optional<Collection> result = adapter.findByReferenceNo("REF-001");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getReferenceNo()).isEqualTo("REF-001");
        assertThat(result.get().getCustomerCode()).isEqualTo("C001");
        assertThat(result.get().getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("존재하지 않거나 null/blank인 referenceNo 조회 시 empty를 반환하고 repository 조회를 건너뛴다")
    void returnsEmptyForNullOrBlankReferenceNo() {
        assertThat(adapter.findByReferenceNo(null)).isEmpty();
        assertThat(adapter.findByReferenceNo("")).isEmpty();
        assertThat(adapter.findByReferenceNo("   ")).isEmpty();

        verify(collectionRepository, never()).findByReferenceNo(org.mockito.ArgumentMatchers.anyString());

        when(collectionRepository.findByReferenceNo("NON_EXISTENT")).thenReturn(Optional.empty());
        assertThat(adapter.findByReferenceNo("NON_EXISTENT")).isEmpty();
    }

    @Test
    @DisplayName("미매칭 수납 목록을 조회하여 도메인 리스트로 반환한다")
    void findsByUnmatchedSuccessfully() {
        CollectionJpaEntity entity = new CollectionJpaEntity();
        entity.setId(11L);
        entity.setStatus(CollectionStatus.UNMATCHED);
        entity.setCustomerCode("C002");
        entity.setAmount(new BigDecimal("300.00"));
        entity.setMatchedAmount(BigDecimal.ZERO);
        entity.setCollectionDate(LocalDate.of(2026, 5, 29));

        when(collectionRepository.findByStatus(CollectionStatus.UNMATCHED)).thenReturn(List.of(entity));

        List<Collection> results = adapter.findByUnmatched();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(11L);
        assertThat(results.get(0).getStatus()).isEqualTo(CollectionStatus.UNMATCHED);
    }
}
