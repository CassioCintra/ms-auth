package io.github.cassio.ms_auth.adapter.in.web.user;

import io.github.cassio.ms_auth.adapter.in.web.user.request.RegisterUserRequest;
import io.github.cassio.ms_auth.adapter.in.web.user.response.UserCreatedResponse;
import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase;
import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase.CreateUserCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    /**
     * Auto-cadastro — qualquer pessoa pode se registrar.
     */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreatedResponse register(@Valid @RequestBody RegisterUserRequest request) {
        log.info("POST /auth/register username={}", request.username());
        return toResponse(createUserUseCase.create(toCommand(request)));
    }

    /**
     * Criação interna — chamado pelo ms-users-management no fluxo de aceite de convite.
     * Requer Bearer token com escopo users:admin.
     */
    @PostMapping("/auth/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreatedResponse createFromInvite(@Valid @RequestBody RegisterUserRequest request) {
        log.info("POST /auth/users username={} (via convite)", request.username());
        return toResponse(createUserUseCase.create(toCommand(request)));
    }

    private CreateUserCommand toCommand(RegisterUserRequest req) {
        return new CreateUserCommand(req.username(), req.email(), req.password(), req.fullName());
    }

    private UserCreatedResponse toResponse(CreateUserUseCase.UserCreatedView view) {
        return new UserCreatedResponse(view.username(), view.email());
    }
}
