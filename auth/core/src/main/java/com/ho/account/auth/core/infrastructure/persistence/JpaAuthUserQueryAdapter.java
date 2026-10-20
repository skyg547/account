package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.model.ListPage;
import com.ho.account.auth.core.application.model.ListPageRequest;
import com.ho.account.auth.core.application.port.out.AuthUserRepository;
import com.ho.account.auth.core.domain.model.AuthUser;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaAuthUserQueryAdapter implements AuthUserRepository {

    private final AuthUserJpaRepository repository;

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsername(username.trim())
                .map(AuthUserJpaEntity::toDomain);
    }

    @Override
    public ListPage<AuthUser> findUsers(ListPageRequest pageRequest) {
        // Page the unique parent keys first: a collection fetch with Pageable limits in memory.
        Slice<String> keys = repository.findPageUsernames(PageRequest.of(pageRequest.page(), pageRequest.size()));
        boolean hasNext = keys.hasNext();
        List<String> selected = keys.getContent();
        if (selected.isEmpty()) {
            return new ListPage<>(List.of(), pageRequest.page(), pageRequest.size(), false);
        }
        Map<String, AuthUser> byName = repository.findByUsernameIn(selected).stream()
                .map(AuthUserJpaEntity::toDomain)
                .collect(Collectors.toMap(AuthUser::getUsername, Function.identity()));
        return new ListPage<>(selected.stream().map(byName::get).filter(Objects::nonNull).toList(),
                pageRequest.page(), pageRequest.size(), hasNext);
    }
}
