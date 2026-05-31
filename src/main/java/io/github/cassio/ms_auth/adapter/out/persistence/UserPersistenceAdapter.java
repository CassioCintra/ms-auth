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
        log.debug("Buscando usuário por username: {}", username);
        return userJpaRepository
                .findByUsername(username)
                .map(UserEntity::toDomain);
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        log.debug("Buscando usuário por email: {}", email);
        return userJpaRepository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public void save(AuthUser user) {
        log.debug("Persistindo usuário: {} roles={}", user.username(), user.authorities());
        userJpaRepository.save(UserEntity.from(user));
    }
}
