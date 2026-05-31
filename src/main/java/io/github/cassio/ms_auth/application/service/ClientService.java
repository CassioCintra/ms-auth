package io.github.cassio.ms_auth.application.service;

import io.github.cassio.ms_auth.domain.client.Client;
import io.github.cassio.ms_auth.domain.client.ClientNotFoundException;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase;
import io.github.cassio.ms_auth.application.port.out.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService implements RegisterClientUseCase {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RegisteredClientView register(RegisterClientCommand command) {
        log.info("Iniciando registro de client OAuth2: clientId={} grantTypes={} scopes={}",
            command.clientId(), command.grantTypes(), command.scopes());

        Client client = new Client(
            UUID.randomUUID().toString(),
            command.clientId(),
            passwordEncoder.encode(command.clientSecret()),
            command.grantTypes(),
            command.scopes(),
            command.redirectUris() != null ? command.redirectUris() : List.of(),
            false
        );
        clientRepository.save(client);

        log.info("Client OAuth2 registrado: clientId={} id={}", client.clientId(), client.id());
        return new RegisteredClientView(client.id(), client.clientId());
    }

    @Override
    public void revoke(String clientId) {
        log.info("Iniciando revogação de client OAuth2: clientId={}", clientId);

        if (!clientRepository.exists(clientId)) {
            log.warn("Revogação negada — client não encontrado: clientId={}", clientId);
            throw new ClientNotFoundException(clientId);
        }

        clientRepository.deleteByClientId(clientId);
        log.info("Client OAuth2 revogado com sucesso: clientId={}", clientId);
    }
}
