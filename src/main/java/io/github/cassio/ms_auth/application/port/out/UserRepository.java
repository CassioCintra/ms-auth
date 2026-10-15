package io.github.cassio.ms_auth.application.port.out;

import io.github.cassio.ms_auth.domain.user.AuthUser;

import java.util.Optional;

public interface UserRepository {

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByEmail(String email);

    boolean existsByUsername(String username);

    void save(AuthUser user);

    void update(AuthUser user);
}
