package io.github.cassio.ms_auth.application.port.in;

import java.util.List;

public interface RegisterClientUseCase {

    record RegisterClientCommand(
        String clientId,
        String clientSecret,
        List<String> grantTypes,
        List<String> scopes,
        List<String> redirectUris
    ) {}

    record RegisteredClientView(String id, String clientId) {}

    RegisteredClientView register(RegisterClientCommand command);

    void revoke(String clientId);
}
