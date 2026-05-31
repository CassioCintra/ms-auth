package io.github.cassio.ms_auth.config;

import io.github.cassio.ms_auth.adapter.out.persistence.UserDetailsAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class TokenCustomizerConfig {

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }
            if (context.getPrincipal() instanceof UsernamePasswordAuthenticationToken auth
                    && auth.getPrincipal() instanceof UserDetailsAdapter details) {
                context.getClaims()
                    .claim("email", details.authUser().email())
                    .claim("name", details.authUser().fullName());
            }
        };
    }
}
