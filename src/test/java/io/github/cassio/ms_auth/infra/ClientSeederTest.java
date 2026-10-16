package io.github.cassio.ms_auth.infra;

import io.github.cassio.ms_auth.TestcontainersConfiguration;
import io.github.cassio.ms_auth.config.TokenCustomizerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "auth.bootstrap.machine-clients.billing-service.secret=configured-secret",
    "auth.bootstrap.machine-clients.billing-service.scopes=flags:read,audit:read",
    "auth.bootstrap.machine-clients.billing-service.workspace-id=7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70",
    "auth.bootstrap.machine-clients.no-secret-service.secret=",
    "auth.bootstrap.machine-clients.no-secret-service.scopes=flags:read",
    "auth.bootstrap.pkce-clients.web-app.redirect-uri=http://localhost/callback"
})
class ClientSeederTest {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    ClientSeeder seeder;

    @Autowired
    RegisteredClientRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterTheConfiguredClientsOnStartup() {
        var machine = repository.findByClientId("billing-service");
        assertThat(machine).isNotNull();
        assertThat(passwordEncoder.matches("configured-secret", machine.getClientSecret())).isTrue();
        assertThat(machine.getScopes()).containsExactlyInAnyOrder("flags:read", "audit:read");
        assertThat(machine.getAuthorizationGrantTypes()).containsExactly(AuthorizationGrantType.CLIENT_CREDENTIALS);
        assertThat(machine.getClientSettings().<String>getSetting(TokenCustomizerConfig.WORKSPACE_ID_SETTING))
                .isEqualTo("7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70");

        var pkce = repository.findByClientId("web-app");
        assertThat(pkce.getRedirectUris()).containsExactly("http://localhost/callback");
        assertThat(pkce.getClientAuthenticationMethods()).containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(pkce.getClientSettings().isRequireProofKey()).isTrue();
    }

    @Test
    void shouldSkipMachineClientsWithoutSecret() {
        assertThat(repository.findByClientId("no-secret-service")).isNull();
    }

    @Test
    void shouldUpdateAnExistingClientToMatchTheConfiguration() {
        var existing = repository.findByClientId("billing-service");
        repository.save(RegisteredClient.from(existing)
                .clientSecret(passwordEncoder.encode("old-secret"))
                .scopes(scopes -> {
                    scopes.clear();
                    scopes.add("users:admin");
                })
                .build());
        var stalePkce = repository.findByClientId("web-app");
        repository.save(RegisteredClient.from(stalePkce)
                .redirectUris(uris -> {
                    uris.clear();
                    uris.add("http://old/callback");
                })
                .build());

        seeder.seed();

        var updated = repository.findByClientId("billing-service");
        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(passwordEncoder.matches("configured-secret", updated.getClientSecret())).isTrue();
        assertThat(passwordEncoder.matches("old-secret", updated.getClientSecret())).isFalse();
        assertThat(updated.getScopes()).containsExactlyInAnyOrder("flags:read", "audit:read");
        assertThat(repository.findByClientId("web-app").getRedirectUris()).containsExactly("http://localhost/callback");
    }
}
