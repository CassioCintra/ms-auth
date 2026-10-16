package io.github.cassio.ms_auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
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

    @Test
    void shouldAddTheWorkspaceOfTheMachineClientToClientCredentialsTokens() {
        var claims = claims();

        sut.customize(clientContext(AuthorizationGrantType.CLIENT_CREDENTIALS, claims, "7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70"));

        assertThat(claims.build().getClaims()).containsEntry("workspace_id", "7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70");
    }

    @Test
    void shouldNotAddAWorkspaceWhenTheClientHasNone() {
        var claims = claims();

        sut.customize(clientContext(AuthorizationGrantType.CLIENT_CREDENTIALS, claims, null));

        assertThat(claims.build().getClaims()).doesNotContainKey("workspace_id");
    }

    @Test
    void shouldNotAddTheClientWorkspaceToUserTokens() {
        var claims = claims();

        sut.customize(clientContext(AuthorizationGrantType.AUTHORIZATION_CODE, claims, "7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70"));

        assertThat(claims.build().getClaims()).doesNotContainKey("workspace_id");
    }

    private static JwtEncodingContext clientContext(AuthorizationGrantType grantType, JwtClaimsSet.Builder claims, String workspaceId) {
        var settings = ClientSettings.builder();
        if (workspaceId != null) settings.setting(TokenCustomizerConfig.WORKSPACE_ID_SETTING, workspaceId);
        var client = RegisteredClient.withId("id").clientId("my-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(grantType)
            .redirectUri("http://localhost/callback")
            .clientSettings(settings.build())
            .build();
        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(grantType)
            .registeredClient(client)
            .authorizedScopes(Set.of("flags:read"))
            .build();
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
