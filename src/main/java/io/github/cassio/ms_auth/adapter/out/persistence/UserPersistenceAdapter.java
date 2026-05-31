package io.github.cassio.ms_auth.adapter.out.persistence;

import io.github.cassio.ms_auth.adapter.out.persistence.entity.UserEntity;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import io.github.cassio.ms_auth.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        log.debug("Querying user by username: {}", username);
        Optional<AuthUser> result = userJpaRepository.findByUsername(username).map(UserEntity::toDomain);
        log.debug("Query by username={}: {}", username, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        log.debug("Querying user by email: {}", email);
        Optional<AuthUser> result = userJpaRepository.findByEmail(email).map(UserEntity::toDomain);
        log.debug("Query by email={}: {}", email, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public void save(AuthUser user) {
        log.debug("Persisting user: username={} roles={}", user.username(), user.authorities());
        userJpaRepository.save(UserEntity.from(user));
        log.debug("User persisted: username={}", user.username());
    }
}
