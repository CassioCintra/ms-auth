package io.github.cassio.ms_auth.adapter.out.persistence;

import io.github.cassio.ms_auth.domain.client.Client;
import io.github.cassio.ms_auth.application.port.out.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientPersistenceAdapter implements ClientRepository {

    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Client client) {
        log.debug("Persistindo client: {} grantTypes={}", client.clientId(), client.grantTypes());

        var builder = RegisteredClient.withId(client.id())
            .clientId(client.clientId())
            .clientSecret(client.encodedSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientSettings(ClientSettings.builder()
                .requireProofKey(client.requireProofKey())
                .build());

        client.grantTypes().stream()
            .map(AuthorizationGrantType::new)
            .forEach(builder::authorizationGrantType);

        client.scopes().forEach(builder::scope);
        client.redirectUris().forEach(builder::redirectUri);

        registeredClientRepository.save(builder.build());
        log.debug("Client persistido: {}", client.clientId());
    }

    @Override
    public boolean exists(String clientId) {
        boolean found = registeredClientRepository.findByClientId(clientId) != null;
        log.debug("Client {} {}", clientId, found ? "encontrado" : "não encontrado");
        return found;
    }

    @Override
    public void deleteByClientId(String clientId) {
        log.debug("Removendo client: {}", clientId);
        jdbcTemplate.update(
            "DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
    }
}
