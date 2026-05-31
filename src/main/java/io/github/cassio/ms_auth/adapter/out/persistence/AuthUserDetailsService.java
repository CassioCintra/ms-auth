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
        log.debug("Carregando credenciais para autenticação: username={}", username);

        return userRepository.findByUsername(username)
            .map(user -> {
                log.info("Credenciais carregadas com sucesso: username={} roles={} enabled={}",
                    user.username(), user.authorities(), user.enabled());
                return (UserDetails) new UserDetailsAdapter(user);
            })
            .orElseThrow(() -> {
                log.warn("Tentativa de autenticação com usuário inexistente: username={}", username);
                return new UsernameNotFoundException("Usuário não encontrado: " + username);
            });
    }
}
