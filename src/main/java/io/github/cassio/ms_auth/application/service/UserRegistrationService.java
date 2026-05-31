package io.github.cassio.ms_auth.application.service;

import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase;
import io.github.cassio.ms_auth.application.port.out.UserRepository;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import io.github.cassio.ms_auth.domain.user.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserCreatedView create(CreateUserCommand command) {
        log.info("Iniciando criação de usuário: username={} email={}", command.username(), command.email());

        userRepository.findByEmail(command.email()).ifPresent(existing -> {
            log.warn("Criação bloqueada — e-mail já cadastrado: email={} username tentado={}",
                command.email(), command.username());
            throw new UserAlreadyExistsException(command.email());
        });

        AuthUser user = new AuthUser(
            command.username(),
            passwordEncoder.encode(command.password()),
            command.email(),
            command.fullName(),
            true,
            List.of("ROLE_USER")
        );
        userRepository.save(user);

        log.info("Usuário criado com sucesso: username={} email={}", user.username(), user.email());
        return new UserCreatedView(user.username(), user.email());
    }
}
