package com.ho.account.masterdata.core.domain.policy;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;

/**
 * 변경 요청 버전과 실제 SCD2 이력 순서를 맞추는 도메인 정책입니다.
 *
 * <p>CREATE는 첫 번째 행인 버전 1, UPDATE는 새 행을 만들기 때문에 현재 이력 수 + 1,
 * DEACTIVATE는 현재 행을 종료하므로 현재 이력 번호를 사용합니다. 요청 시점뿐 아니라
 * 승인과 반영 직전에도 확인해야 대기 중 다른 변경이 먼저 반영된 경우를 막을 수 있습니다.</p>
 */
public final class MasterDataChangeVersionPolicy {

    private MasterDataChangeVersionPolicy() {
    }

    public static void verify(ChangeType changeType, int requestedVersion, long persistedVersionCount) {
        if (persistedVersionCount < 0) {
            throw new IllegalArgumentException("Persisted version count cannot be negative.");
        }

        long expectedVersion = expectedVersion(changeType, persistedVersionCount);
        if (requestedVersion != expectedVersion) {
            throw new MasterDataVersionConflictException(
                    "Master-data version conflict. expected=" + expectedVersion
                            + ", requested=" + requestedVersion
                            + ", persistedVersions=" + persistedVersionCount);
        }
    }

    private static long expectedVersion(ChangeType changeType, long persistedVersionCount) {
        return switch (changeType) {
            case CREATE -> {
                if (persistedVersionCount != 0) {
                    throw new MasterDataVersionConflictException(
                            "CREATE is allowed only when no SCD2 history exists for the target key.");
                }
                yield 1;
            }
            case UPDATE -> {
                requireExistingHistory(persistedVersionCount, changeType);
                yield Math.addExact(persistedVersionCount, 1);
            }
            case DEACTIVATE -> {
                requireExistingHistory(persistedVersionCount, changeType);
                yield persistedVersionCount;
            }
        };
    }

    private static void requireExistingHistory(long persistedVersionCount, ChangeType changeType) {
        if (persistedVersionCount == 0) {
            throw new MasterDataVersionConflictException(changeType + " requires an existing SCD2 history.");
        }
    }
}