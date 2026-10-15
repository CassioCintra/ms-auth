package io.github.cassio.ms_auth.config;

import io.github.cassio.ms_auth.adapter.out.persistence.UserDetailsAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
@EnableConfigurationProperties(TokenAudienceProperties.class)
public class TokenCustomizerConfig {

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(TokenAudienceProperties audienceProperties) {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }
            // aud = recursos dos escopos concedidos (ex.: flags:read -> consul), no lugar do client_id
            List<String> audiences = context.getAuthorizedScopes().stream()
                .map(audienceProperties.audiences()::get)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
            if (!audiences.isEmpty()) {
                log.debug("Setting access token audience: scopes={} aud={}",
                    context.getAuthorizedScopes(), audiences);
                context.getClaims().audience(audiences);
            }
            if (context.getPrincipal() instanceof UsernamePasswordAuthenticationToken auth
                    && auth.getPrincipal() instanceof UserDetailsAdapter details) {
                log.debug("Enriching access token with user claims: username={} email={}",
                    details.authUser().username(), details.authUser().email());
                context.getClaims()
                    .claim("email", details.authUser().email())
                    .claim("name", details.authUser().fullName());
            }
        };
    }
}
