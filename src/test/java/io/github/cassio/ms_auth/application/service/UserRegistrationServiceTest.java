package io.github.cassio.ms_auth.application.service;

import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase;
import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase.CreateUserCommand;
import io.github.cassio.ms_auth.application.port.out.UserRepository;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import io.github.cassio.ms_auth.domain.user.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserRegistrationService sut;

    @Test
    void shouldCreateUserWhenEmailDoesNotExist() {
        var command = new CreateUserCommand("john", "john@example.com", "secret123", "John Doe");
        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret123")).willReturn("hashed");

        CreateUserUseCase.UserCreatedView result = sut.create(command);

        assertThat(result.username()).isEqualTo("john");
        assertThat(result.email()).isEqualTo("john@example.com");
        then(userRepository).should().save(argThat(u ->
            u.username().equals("john") &&
            u.password().equals("hashed") &&
            u.authorities().contains("ROLE_USER")));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        var command = new CreateUserCommand("john", "john@example.com", "secret123", "John Doe");
        given(userRepository.findByEmail("john@example.com"))
            .willReturn(Optional.of(new AuthUser("john", "hashed", "john@example.com", "John Doe", true, java.util.List.of())));

        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(UserAlreadyExistsException.class);
        then(userRepository).should(never()).save(argThat(u -> true));
    }
}
