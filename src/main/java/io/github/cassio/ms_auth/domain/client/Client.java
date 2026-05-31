package io.github.cassio.ms_auth.domain.client;

import java.util.List;

public record Client(
    String id,
    String clientId,
    String encodedSecret,
    List<String> grantTypes,
    List<String> scopes,
    List<String> redirectUris,
    boolean requireProofKey
) {}
