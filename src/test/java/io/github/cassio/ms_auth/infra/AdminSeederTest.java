package io.github.cassio.ms_auth.infra;

import io.github.cassio.ms_auth.TestcontainersConfiguration;
import io.github.cassio.ms_auth.application.port.out.UserRepository;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "auth.bootstrap.admin.username=root-admin",
    "auth.bootstrap.admin.password=configured-password"
})
class AdminSeederTest {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    AdminSeeder seeder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateTheConfiguredAdminOnStartup() {
        var admin = userRepository.findByUsername("root-admin").orElseThrow();

        assertThat(passwordEncoder.matches("configured-password", admin.password())).isTrue();
        assertThat(admin.authorities()).contains("ROLE_USER", "ROLE_AUTH_ADMIN");
        assertThat(admin.enabled()).isTrue();
    }

    @Test
    void shouldUpdateTheExistingAdminAndKeepItsOtherData() {
        var current = userRepository.findByUsername("root-admin").orElseThrow();
        userRepository.update(new AuthUser("root-admin", passwordEncoder.encode("old-password"),
                "root@example.com", "Root", false, List.of("ROLE_USER", "ROLE_AUDITOR")));

        seeder.seed();

        var updated = userRepository.findByUsername("root-admin").orElseThrow();
        assertThat(passwordEncoder.matches("configured-password", updated.password())).isTrue();
        assertThat(updated.authorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_AUDITOR", "ROLE_AUTH_ADMIN");
        assertThat(updated.enabled()).isTrue();
        assertThat(updated.email()).isEqualTo("root@example.com");
        assertThat(updated.fullName()).isEqualTo("Root");
        assertThat(current.username()).isEqualTo(updated.username());
    }
}
