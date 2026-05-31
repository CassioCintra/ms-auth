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
        log.debug("Carregando usuário: {}", username);

        return userRepository.findByUsername(username)
            .map(user -> {
                log.info("Usuário autenticado: {} roles={}", user.username(), user.authorities());
                return (UserDetails) new UserDetailsAdapter(user);
            })
            .orElseThrow(() -> {
                log.warn("Usuário não encontrado: {}", username);
                return new UsernameNotFoundException("Usuário não encontrado: " + username);
            });
    }
}
