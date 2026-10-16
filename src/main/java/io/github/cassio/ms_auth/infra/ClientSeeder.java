package io.github.cassio.ms_auth.infra;

import io.github.cassio.ms_auth.config.TokenCustomizerConfig;
import io.github.cassio.ms_auth.infra.BootstrapProperties.MachineClient;
import io.github.cassio.ms_auth.infra.BootstrapProperties.PkceClient;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientSeeder {

    private final RegisteredClientRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        properties.machineClients().forEach(this::registerMachineClient);
        properties.pkceClients().forEach(this::registerPkceClient);
    }

    private void registerMachineClient(String clientId, MachineClient client) {
        if (client.secret() == null || client.secret().isBlank()) {
            log.warn("OAuth2 client skipped, no secret configured: clientId={}", clientId);
            return;
        }
        var existing = repository.findByClientId(clientId);
        var registeredClient = builderFor(existing, clientId)
                .clientSecret(passwordEncoder.encode(client.secret()))
                .clientAuthenticationMethods(methods -> replace(methods, List.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                .authorizationGrantTypes(grants -> replace(grants, List.of(AuthorizationGrantType.CLIENT_CREDENTIALS)))
                .scopes(scopes -> replace(scopes, client.scopes()))
                .clientSettings(machineClientSettings(client))
                .build();
        repository.save(registeredClient);
        log.info("OAuth2 client {}: clientId={} scopes={} workspaceId={}",
                existing == null ? "registered" : "updated", clientId, client.scopes(), client.workspaceId());
    }

    private void registerPkceClient(String clientId, PkceClient client) {
        var existing = repository.findByClientId(clientId);
        var registeredClient = builderFor(existing, clientId)
                .clientAuthenticationMethods(methods -> replace(methods, List.of(ClientAuthenticationMethod.NONE)))
                .authorizationGrantTypes(grants -> replace(grants,
                        List.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN)))
                .redirectUris(uris -> replace(uris, List.of(client.redirectUri())))
                .scopes(scopes -> replace(scopes, List.of(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)))
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .build();
        repository.save(registeredClient);
        log.info("OAuth2 PKCE client {}: clientId={} redirectUri={}",
                existing == null ? "registered" : "updated", clientId, client.redirectUri());
    }

    private static ClientSettings machineClientSettings(MachineClient client) {
        var settings = ClientSettings.builder();
        if (client.workspaceId() != null) {
            settings.setting(TokenCustomizerConfig.WORKSPACE_ID_SETTING, client.workspaceId().toString());
        }
        return settings.build();
    }

    private static RegisteredClient.Builder builderFor(RegisteredClient existing, String clientId) {
        return existing != null
                ? RegisteredClient.from(existing)
                : RegisteredClient.withId(UUID.randomUUID().toString()).clientId(clientId);
    }

    private static <T> void replace(Set<T> target, List<T> values) {
        target.clear();
        target.addAll(values);
    }
}
