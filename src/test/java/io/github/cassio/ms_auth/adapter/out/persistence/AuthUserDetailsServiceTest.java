package io.github.cassio.ms_auth.adapter.out.persistence;

import io.github.cassio.ms_auth.application.port.out.UserRepository;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthUserDetailsServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks AuthUserDetailsService sut;

    @Test
    void shouldReturnUserDetailsWhenUserExists() {
        var user = new AuthUser("alice", "hashed", "alice@example.com", "Alice", true,
            List.of("ROLE_USER"));
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user));

        UserDetails details = sut.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("ghost");
    }
}
