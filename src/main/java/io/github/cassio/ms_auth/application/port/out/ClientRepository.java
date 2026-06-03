package io.github.cassio.ms_auth.application.port.out;

import io.github.cassio.ms_auth.domain.client.Client;

public interface ClientRepository {

    void save(Client client);

    boolean exists(String clientId);

    void deleteByClientId(String clientId);
}
