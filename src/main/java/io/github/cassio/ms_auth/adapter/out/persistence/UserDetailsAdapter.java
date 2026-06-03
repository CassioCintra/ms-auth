package io.github.cassio.ms_auth.adapter.out.persistence;

import io.github.cassio.ms_auth.domain.user.AuthUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public record UserDetailsAdapter(AuthUser authUser) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authUser.authorities().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    @Override
    public String getPassword() { return authUser.password(); }

    @Override
    public String getUsername() { return authUser.username(); }

    @Override
    public boolean isEnabled() { return authUser.enabled(); }

}
