package io.github.cassio.ms_auth.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientSeeder {

    private final RegisteredClientRepository repository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        registerMachineClient("order-service",           "flags:read");
        registerMachineClient("ms-workspace-management", "users:admin");
        registerMachineClient("ms-dashboard",            "flags:read", "audit:read");
        registerPkceClient();
    }

    private void registerMachineClient(String clientId, String... scopes) {
        if (repository.findByClientId(clientId) != null) {
            return;
        }
        var builder = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientId + "-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
        for (String scope : scopes) {
            builder.scope(scope);
        }
        repository.save(builder.build());
        log.info("Client registrado: {}", clientId);
    }

    private void registerPkceClient() {
        if (repository.findByClientId("dashboard-frontend") != null) {
            return;
        }
        var builder = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("dashboard-frontend")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://app.switchboard.io/callback")
            .clientSettings(ClientSettings.builder().requireProofKey(true).build());
        Stream.of(
                OidcScopes.OPENID,
                OidcScopes.PROFILE,
                OidcScopes.EMAIL
        ).forEach(builder::scope);
        repository.save(builder.build());
        log.info("Client PKCE registrado: {}", "dashboard-frontend");
    }
}
