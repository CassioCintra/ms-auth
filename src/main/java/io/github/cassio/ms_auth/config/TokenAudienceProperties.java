package io.github.cassio.ms_auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Escopo -> recurso que deve aceitar o token (claim {@code aud} do access token).
 * Ex.: {@code flags:read -> consul}. Escopos sem mapeamento mantêm o aud padrão (client_id).
 */
@ConfigurationProperties(prefix = "auth")
public record TokenAudienceProperties(Map<String, String> audiences) {

    public TokenAudienceProperties {
        audiences = audiences == null ? Map.of() : Map.copyOf(audiences);
    }
}
