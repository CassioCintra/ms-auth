package io.github.cassio.ms_auth.adapter.out.persistence;

import io.github.cassio.ms_auth.TestcontainersConfiguration;
import io.github.cassio.ms_auth.adapter.out.persistence.entity.UserEntity;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class UserPersistenceAdapterTest {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    UserPersistenceAdapter sut;

    @Autowired
    UserJpaRepository userJpaRepository;

    @Test
    void shouldReturnUserWhenUsernameExists() {
        userJpaRepository.save(buildEntity("alice_test", "alice_test@example.com", "ROLE_USER"));

        Optional<AuthUser> result = sut.findByUsername("alice_test");

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("alice_test");
        assertThat(result.get().email()).isEqualTo("alice_test@example.com");
        assertThat(result.get().authorities()).contains("ROLE_USER");
    }

    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        Optional<AuthUser> result = sut.findByUsername("nonexistent_xyz");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenUserExists() {
        userJpaRepository.save(buildEntity("bob_test", "bob_test@example.com", "ROLE_USER"));

        assertThat(sut.existsByUsername("bob_test")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExist() {
        assertThat(sut.existsByUsername("nobody_xyz")).isFalse();
    }

    @Test
    void shouldSaveUserWithAuthorities() {
        AuthUser user = new AuthUser(
            "carol_test", "hashed", "carol_test@example.com", "Carol Test", true,
            List.of("ROLE_USER", "ROLE_AUTH_ADMIN"));

        sut.save(user);

        Optional<UserEntity> saved = userJpaRepository.findByUsername("carol_test");
        assertThat(saved).isPresent();
        assertThat(saved.get().getAuthorities())
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_AUTH_ADMIN");
    }

    private UserEntity buildEntity(String username, String email, String... roles) {
        UserEntity entity = new UserEntity(username, "hashed", email, username);
        entity.setAuthorities(List.of(roles));
        return entity;
    }
}
