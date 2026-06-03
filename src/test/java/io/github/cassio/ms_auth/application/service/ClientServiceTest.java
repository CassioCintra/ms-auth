package io.github.cassio.ms_auth.application.service;

import io.github.cassio.ms_auth.domain.client.Client;
import io.github.cassio.ms_auth.domain.client.ClientNotFoundException;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase.RegisterClientCommand;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase.RegisteredClientView;
import io.github.cassio.ms_auth.application.port.out.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks ClientService sut;

    @Test
    void shouldRegisterClientWhenCommandIsValid() {
        var command = new RegisterClientCommand(
            "svc-a", "raw-secret", List.of("client_credentials"), List.of("flags:read"), null);
        given(passwordEncoder.encode("raw-secret")).willReturn("hashed-secret");

        RegisteredClientView result = sut.register(command);

        assertThat(result.clientId()).isEqualTo("svc-a");
        assertThat(result.id()).isNotBlank();
        then(clientRepository).should().save(argThat(c ->
            c.clientId().equals("svc-a") && c.encodedSecret().equals("hashed-secret")));
    }

    @Test
    void shouldRevokeClientWhenClientExists() {
        given(clientRepository.exists("svc-a")).willReturn(true);

        sut.revoke("svc-a");

        then(clientRepository).should().deleteByClientId("svc-a");
    }

    @Test
    void shouldThrowWhenRevokingNonExistentClient() {
        given(clientRepository.exists("ghost")).willReturn(false);

        assertThatThrownBy(() -> sut.revoke("ghost"))
            .isInstanceOf(ClientNotFoundException.class);
        then(clientRepository).should(never()).deleteByClientId("ghost");
    }
}
