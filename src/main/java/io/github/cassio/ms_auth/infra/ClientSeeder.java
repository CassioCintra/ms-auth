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
        registerMachineClient("ms-feature-flags-management", "flags:write");
        registerPkceClient("dashboard-frontend", "http://app.switchboard.io/callback");
        registerPkceClient("swagger-ui",         "http://localhost:8090/swagger-ui/oauth2-redirect.html");
    }

    private void registerMachineClient(String clientId, String... scopes) {
        if (repository.findByClientId(clientId) != null) {
            log.debug("Client already registered, skipping: clientId={}", clientId);
            return;
        }
        String secret = clientId + "-secret";
        var builder = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
        for (String scope : scopes) {
            builder.scope(scope);
        }
        repository.save(builder.build());
        log.info("OAuth2 client registered: clientId={} scopes={} secret={}", clientId, scopes, secret);
    }

    private void registerPkceClient(String clientId, String redirectUri) {
        if (repository.findByClientId(clientId) != null) {
            log.debug("Client already registered, skipping: clientId={}", clientId);
            return;
        }
        var builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .clientSettings(ClientSettings.builder().requireProofKey(true).build());
        Stream.of(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL).forEach(builder::scope);
        repository.save(builder.build());
        log.info("OAuth2 PKCE client registered: clientId={} redirectUri={}", clientId, redirectUri);
    }
}
