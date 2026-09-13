package com.ho.account.masterdata.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link BusinessPartner}가 저장소나 Spring 없이도 핵심 거래처 규칙을 지키는지 검증합니다.
 *
 * <p>이 테스트가 순수 단위 테스트인 이유는 필수값, 기본값, SCD2 유효기간 같은 업무 규칙이
 * Controller나 JPA 설정에 우연히 의존하지 않아야 하기 때문입니다. 회귀가 생기면 인프라 문제와
 * 구분해 도메인 계약 자체의 문제임을 빠르게 알 수 있습니다.</p>
 */
class BusinessPartnerTest {

    @Test
    void createNormalizesRequiredValuesAndAppliesSafeDefaults() {
        LocalDate beforeCreation = LocalDate.now();

        BusinessPartner partner = BusinessPartner.create(
                " BP-001 ",
                " 새봄상사 ",
                " 123-45-67890 ",
                " ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        LocalDate afterCreation = LocalDate.now();

        // 모든 진입점이 같은 factory를 사용하면 누락된 선택값을 DB 기본값에 맡기지 않아도 됩니다.
        assertThat(partner.getBusinessPartnerCode()).isEqualTo("BP-001");
        assertThat(partner.getBusinessPartnerName()).isEqualTo("새봄상사");
        assertThat(partner.getRegistrationNumber()).isEqualTo("123-45-67890");
        assertThat(partner.getCeoName()).isNull();
        assertThat(partner.getPartnerType()).isEqualTo(BusinessPartner.PartnerType.OTHER_BP);
        assertThat(partner.getUseYn()).isTrue();
        assertThat(partner.getKycStatus()).isEqualTo(BusinessPartner.KycStatus.PENDING);
        assertThat(partner.getRiskRating()).isEqualTo(BusinessPartner.RiskRating.LOW);
        assertThat(partner.getValidFrom()).isBetween(beforeCreation, afterCreation);
        assertThat(partner.getValidTo()).isEqualTo(BusinessPartner.OPEN_ENDED_VALID_TO);
        assertThat(partner.getAuditUser()).isEqualTo("SYSTEM");
        assertThat(partner.getAccounts()).isEmpty();
        assertThat(partner.getCreatedAt()).isNotNull();
        assertThat(partner.getUpdatedAt()).isNotNull();
    }

    @Test
    void createRejectsMissingBusinessIdentity() {
        // 코드와 이름은 SCD2 이력을 연결하고 사용자에게 표시하는 필수 업무 식별 정보입니다.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createWithIdentity(" ", "새봄상사"))
                .withMessage("거래처 코드는 필수입니다.");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> createWithIdentity("BP-001", null))
                .withMessage("거래처명은 필수입니다.");
    }

    @Test
    void validityWindowIsClosedAndCannotBeReversed() {
        LocalDate validFrom = LocalDate.of(2026, 1, 1);
        LocalDate validTo = LocalDate.of(2026, 12, 31);
        BusinessPartner partner = createForWindow(validFrom, validTo);

        // SCD2 구간은 양 끝 날짜를 모두 포함하므로 월말 당일 조회가 빠지지 않아야 합니다.
        assertThat(partner.isValid(validFrom)).isTrue();
        assertThat(partner.isValid(validTo)).isTrue();
        assertThat(partner.isValid(validFrom.minusDays(1))).isFalse();
        assertThat(partner.isValid(validTo.plusDays(1))).isFalse();
        assertThat(partner.isValid(null)).isFalse();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> createForWindow(validTo, validFrom))
                .withMessage("유효 종료일은 유효 시작일보다 빠를 수 없습니다.");

        // 복원 경로도 손상된 저장 데이터를 정상 aggregate로 받아들이면 안 됩니다.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> BusinessPartner.reconstitute(
                        1L,
                        "BP-001",
                        "새봄상사",
                        null,
                        null,
                        null,
                        null,
                        BusinessPartner.PartnerType.VENDOR,
                        true,
                        BusinessPartner.KycStatus.APPROVED,
                        BusinessPartner.RiskRating.LOW,
                        null,
                        validTo,
                        null,
                        null,
                        "SYSTEM",
                        null))
                .withMessage("유효 시작일은 필수입니다.");
    }

    @Test
    void terminateAcceptsTheStartBoundaryAndIsIdempotentOnlyForTheSameEndDate() {
        LocalDate validFrom = LocalDate.of(2026, 1, 1);
        LocalDate validTo = LocalDate.of(2026, 12, 31);
        BusinessPartner partner = createForWindow(validFrom, validTo);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> partner.terminate(validFrom.minusDays(1)))
                .withMessage("거래처 종료일은 유효 시작일보다 빠를 수 없습니다.");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> partner.terminate(validTo.plusDays(1)))
                .withMessage("거래처 종료일은 현재 유효 종료일보다 늦을 수 없습니다.");

        // 시작일 당일 종료도 유효한 하루짜리 닫힌 구간이며, 같은 재시도는 안전해야 합니다.
        partner.terminate(validFrom);
        partner.terminate(validFrom);

        assertThat(partner.getValidTo()).isEqualTo(validFrom);
        assertThat(partner.getUseYn()).isTrue();
        assertThat(partner.isActiveAt(validFrom)).isTrue();
        assertThat(partner.isActiveAt(validFrom.plusDays(1))).isFalse();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> partner.terminate(validFrom.plusDays(1)))
                .withMessage("거래처 종료일은 현재 유효 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void nextScd2VersionCopiesAccountsAsNewChildrenWithoutMovingHistory() {
        LocalDate today = LocalDate.now();
        LocalDateTime oldTimestamp = LocalDateTime.of(2025, 12, 31, 10, 0);
        BusinessPartnerAccount historicalAccount = BusinessPartnerAccount.reconstitute(
                77L,
                "한국은행",
                "110-123-456789",
                "새봄상사",
                "BOKRKRSE",
                true,
                oldTimestamp,
                oldTimestamp);
        BusinessPartner current = BusinessPartner.reconstitute(
                10L,
                "BP-ACCOUNT-COPY",
                "새봄상사",
                "123-45-67890",
                "김새봄",
                "도매업",
                "원자재",
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                today.minusDays(30),
                BusinessPartner.OPEN_ENDED_VALID_TO,
                oldTimestamp,
                oldTimestamp,
                "tester",
                List.of(historicalAccount));

        BusinessPartner next = current.createNextVersion(
                "BP-ACCOUNT-COPY",
                "새봄상사 신사명",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                today.plusDays(1),
                BusinessPartner.OPEN_ENDED_VALID_TO);
        current.closeVersion(next.getValidFrom().minusDays(1));

        BusinessPartnerAccount copiedAccount = next.getAccounts().get(0);
        // 과거 자식의 ID와 부모는 그대로 두고, 다음 버전은 같은 업무 값을 가진 신규 자식 행을 소유합니다.
        assertThat(current.getValidTo()).isEqualTo(today);
        assertThat(current.getUseYn()).isTrue();
        assertThat(next.getBusinessPartnerCode()).isEqualTo(current.getBusinessPartnerCode());
        assertThat(next.getBusinessPartnerName()).isEqualTo("새봄상사 신사명");
        assertThat(next.getValidFrom()).isEqualTo(today.plusDays(1));
        assertThat(next.getValidTo()).isEqualTo(BusinessPartner.OPEN_ENDED_VALID_TO);
        assertThat(historicalAccount.getId()).isEqualTo(77L);
        assertThat(historicalAccount.getBusinessPartner()).isSameAs(current);
        assertThat(copiedAccount).isNotSameAs(historicalAccount);
        assertThat(copiedAccount.getId()).isNull();
        assertThat(copiedAccount.getAccountNumber()).isEqualTo(historicalAccount.getAccountNumber());
        assertThat(copiedAccount.isMainAccount()).isTrue();
        assertThat(copiedAccount.getBusinessPartner()).isSameAs(next);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 21, 30})
    void nextVersionCannotEscapeCurrentWindowAfterAFutureSplit(int secondStart) {
        LocalDate today = LocalDate.now();
        BusinessPartner current = createForWindow(today.minusDays(30), BusinessPartner.OPEN_ENDED_VALID_TO);
        BusinessPartner scheduled = nextVersion(current, today.plusDays(21), BusinessPartner.OPEN_ENDED_VALID_TO);
        current.closeVersion(today.plusDays(20));
        LocalDateTime currentUpdatedAt = current.getUpdatedAt();
        LocalDateTime scheduledUpdatedAt = scheduled.getUpdatedAt();

        assertThatIllegalArgumentException().isThrownBy(() ->
                nextVersion(current, today.plusDays(secondStart), BusinessPartner.OPEN_ENDED_VALID_TO));

        assertThat(current.getValidFrom()).isEqualTo(today.minusDays(30));
        assertThat(current.getValidTo()).isEqualTo(today.plusDays(20));
        assertThat(current.getUpdatedAt()).isEqualTo(currentUpdatedAt);
        assertThat(current.isActiveAt(today)).isTrue();
        assertThat(scheduled.getValidFrom()).isEqualTo(today.plusDays(21));
        assertThat(scheduled.getValidTo()).isEqualTo(BusinessPartner.OPEN_ENDED_VALID_TO);
        assertThat(scheduled.getUpdatedAt()).isEqualTo(scheduledUpdatedAt);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20})
    void nextVersionCanSplitInsideCurrentWindowIncludingItsLastDay(int secondStart) {
        LocalDate today = LocalDate.now();
        BusinessPartner current = createForWindow(today.minusDays(30), BusinessPartner.OPEN_ENDED_VALID_TO);
        BusinessPartner scheduled = nextVersion(current, today.plusDays(21), BusinessPartner.OPEN_ENDED_VALID_TO);
        current.closeVersion(today.plusDays(20));

        BusinessPartner inserted = nextVersion(current, today.plusDays(secondStart), today.plusDays(20));
        // Creation must remain side-effect free until the service explicitly closes the old version.
        assertThat(current.getValidTo()).isEqualTo(today.plusDays(20));
        current.closeVersion(inserted.getValidFrom().minusDays(1));

        List<BusinessPartner> versions = List.of(current, inserted, scheduled);
        for (LocalDate date = today.minusDays(30); !date.isAfter(today.plusDays(60)); date = date.plusDays(1)) {
            LocalDate asOf = date;
            assertThat(versions.stream().filter(version -> version.isValid(asOf)).count())
                    .as("valid versions at %s", date).isEqualTo(1);
        }
        assertThat(inserted.getValidFrom()).isEqualTo(today.plusDays(secondStart));
        assertThat(inserted.getValidTo()).isEqualTo(today.plusDays(20));
        assertThat(scheduled.getValidFrom()).isEqualTo(today.plusDays(21));
        assertThat(scheduled.getValidTo()).isEqualTo(BusinessPartner.OPEN_ENDED_VALID_TO);
    }

    @Test
    void nextVersionRejectsInvalidSplitWithoutChangingCurrentVersion() {
        LocalDate today = LocalDate.now();
        BusinessPartner current = createForWindow(today.minusDays(30), BusinessPartner.OPEN_ENDED_VALID_TO);
        LocalDateTime updatedAt = current.getUpdatedAt();

        assertThatIllegalArgumentException().isThrownBy(() ->
                nextVersion(current, current.getValidFrom(), BusinessPartner.OPEN_ENDED_VALID_TO));
        assertThatIllegalArgumentException().isThrownBy(() ->
                nextVersion(current, today.plusDays(10), today.plusDays(9)));

        assertThat(current.getValidTo()).isEqualTo(BusinessPartner.OPEN_ENDED_VALID_TO);
        assertThat(current.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private BusinessPartner nextVersion(BusinessPartner current, LocalDate from, LocalDate to) {
        return current.createNextVersion(current.getBusinessPartnerCode(), "Revised", null, null,
                null, null, null, null, null, null, from, to);
    }

    private BusinessPartner createWithIdentity(String code, String name) {
        return BusinessPartner.create(
                code,
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                BusinessPartner.OPEN_ENDED_VALID_TO);
    }

    private BusinessPartner createForWindow(LocalDate validFrom, LocalDate validTo) {
        return BusinessPartner.create(
                "BP-001",
                "새봄상사",
                "123-45-67890",
                "김새봄",
                "도매업",
                "원자재",
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                validFrom,
                validTo);
    }
}
