package io.github.cassio.ms_auth.adapter.out.persistence;

import io.github.cassio.ms_auth.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading credentials for authentication: username={}", username);

        return userRepository.findByUsername(username)
            .map(user -> {
                log.info("Credentials loaded: username={} roles={} enabled={}",
                    user.username(), user.authorities(), user.enabled());
                return (UserDetails) new UserDetailsAdapter(user);
            })
            .orElseThrow(() -> {
                log.warn("Authentication attempt for unknown user: username={}", username);
                return new UsernameNotFoundException("User not found: " + username);
            });
    }
}
