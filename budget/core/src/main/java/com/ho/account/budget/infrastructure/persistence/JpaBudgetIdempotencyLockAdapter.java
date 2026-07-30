package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.application.port.out.BudgetIdempotencyLockPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 동일 업무키를 256개 영구 shard 중 하나의 비관적 잠금으로 직렬화합니다.
 *
 * <p>SHA-256은 JVM 실행마다 바뀌지 않으므로 모든 인스턴스가 같은 키를 같은 shard로
 * 보냅니다. 서로 다른 키의 shard 충돌은 정확성을 해치지 않고 잠금 경합만 늘립니다.</p>
 */
@Component
public class JpaBudgetIdempotencyLockAdapter implements BudgetIdempotencyLockPort {

    private static final int SHARD_COUNT = 256;
    private static final String TRANSFER_NAMESPACE = "TRANSFER";
    private static final String EXECUTION_NAMESPACE = "EXECUTION";

    private final SpringDataBudgetIdempotencyShardRepository repository;

    public JpaBudgetIdempotencyLockAdapter(
            SpringDataBudgetIdempotencyShardRepository repository) {
        this.repository = repository;
    }

    @Override
    public void lockTransferRequestKey(String requestKey) {
        lockShard(canonical(TRANSFER_NAMESPACE, requestKey));
    }

    @Override
    public void lockExecutionSourceKey(
            String sourceType, String sourceId, String sourceLineId) {
        lockShard(canonical(EXECUTION_NAMESPACE, sourceType, sourceId, sourceLineId));
    }

    private void lockShard(String canonicalKey) {
        short shardId = (short) stableShard(canonicalKey);
        repository.findByShardIdForUpdate(shardId)
                .orElseThrow(() -> new IllegalStateException(
                        "사전 생성된 idempotency shard 행이 없습니다: " + shardId));
    }

    private int stableShard(String canonicalKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalKey.getBytes(StandardCharsets.UTF_8));
            return Byte.toUnsignedInt(digest[0]) % SHARD_COUNT;
        } catch (NoSuchAlgorithmException exception) {
            // 모든 Java 17 구현은 SHA-256을 제공하므로 이 분기는 런타임 손상만 의미합니다.
            throw new IllegalStateException("SHA-256 digest를 사용할 수 없습니다.", exception);
        }
    }

    private String canonical(String namespace, String... components) {
        StringBuilder key = new StringBuilder(namespace).append(':');
        for (String component : components) {
            String required = Objects.requireNonNull(component, "idempotency key component");
            key.append(required.length()).append(':').append(required);
        }
        return key.toString();
    }
}
