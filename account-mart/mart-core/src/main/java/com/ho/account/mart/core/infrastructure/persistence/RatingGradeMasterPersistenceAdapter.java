package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.RatingGradeMasterRepository;
import com.ho.account.mart.core.domain.external.kap.RatingGradeMaster;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RatingGradeMasterPersistenceAdapter implements RatingGradeMasterRepository {

    private final Map<String, RatingGradeMaster> gradeMasters = new ConcurrentHashMap<>();

    public RatingGradeMasterPersistenceAdapter() {
        seed("KAP", "AAA", "AAA", 1);
        seed("KAP", "AA+", "AA+", 2);
        seed("KAP", "AA", "AA", 3);
        seed("KAP", "AA-", "AA-", 4);
        seed("KAP", "A+", "A+", 5);
        seed("KAP", "A", "A", 6);
        seed("KAP", "A-", "A-", 7);
        seed("KAP", "BBB", "BBB", 8);
        seed("KAP", "BB", "BB", 9);
        seed("KAP", "B", "B", 10);
        seed("KAP", "CCC", "CCC", 11);
        seed("KAP", "NR", "Not Rated", 99);
    }

    @Override
    public Optional<RatingGradeMaster> findByGradeCodeIgnoreCaseAndIsActiveTrue(String gradeCode) {
        if (gradeCode == null) {
            return Optional.empty();
        }
        RatingGradeMaster gradeMaster = gradeMasters.get(normalizeKey(gradeCode));
        if (gradeMaster == null || !Boolean.TRUE.equals(gradeMaster.getIsActive())) {
            return Optional.empty();
        }
        return Optional.of(gradeMaster);
    }

    @Override
    public List<RatingGradeMaster> findAll() {
        return new ArrayList<>(gradeMasters.values());
    }

    @Override
    public RatingGradeMaster save(RatingGradeMaster gradeMaster) {
        gradeMasters.put(normalizeKey(gradeMaster.getGradeCode()), gradeMaster);
        return gradeMaster;
    }

    private void seed(String agency, String code, String name, int score) {
        save(RatingGradeMaster.builder()
                .id((long) score)
                .ratingAgency(agency)
                .gradeCode(code)
                .gradeName(name)
                .gradeScore(score)
                .isActive(true)
                .build());
    }

    private String normalizeKey(String gradeCode) {
        return gradeCode.toUpperCase(Locale.ROOT);
    }
}
