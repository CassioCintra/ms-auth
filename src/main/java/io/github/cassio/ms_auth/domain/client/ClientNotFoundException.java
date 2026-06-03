package io.github.cassio.ms_auth.domain.client;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String clientId) {
        super("Client not found: " + clientId);
    }
}
