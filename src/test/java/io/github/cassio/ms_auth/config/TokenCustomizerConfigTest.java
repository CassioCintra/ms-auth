package io.github.cassio.ms_auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenCustomizerConfigTest {

    OAuth2TokenCustomizer<JwtEncodingContext> sut = new TokenCustomizerConfig().tokenCustomizer(
        new TokenAudienceProperties(Map.of("flags:read", "consul", "flags:write", "consul")));

    @Test
    void shouldReplaceAudienceWithMappedResourceForFlagScopes() {
        var claims = claims();

        sut.customize(context(OAuth2TokenType.ACCESS_TOKEN, claims, Set.of("flags:read", "flags:write")));

        assertThat(claims.build().getAudience()).containsExactly("consul");
    }

    @Test
    void shouldKeepDefaultAudienceForUnmappedScopes() {
        var claims = claims();

        sut.customize(context(OAuth2TokenType.ACCESS_TOKEN, claims, Set.of("users:admin")));

        assertThat(claims.build().getAudience()).containsExactly("my-client");
    }

    @Test
    void shouldNotChangeAudienceOfNonAccessTokens() {
        var claims = claims();

        sut.customize(context(OAuth2TokenType.REFRESH_TOKEN, claims, Set.of("flags:read")));

        assertThat(claims.build().getAudience()).containsExactly("my-client");
    }

    // Padrão do Spring Authorization Server: aud = client_id
    private static JwtClaimsSet.Builder claims() {
        return JwtClaimsSet.builder().subject("my-client").audience(List.of("my-client"));
    }

    private static JwtEncodingContext context(OAuth2TokenType type, JwtClaimsSet.Builder claims, Set<String> scopes) {
        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
            .tokenType(type)
            .authorizedScopes(scopes)
            .build();
    }
}
