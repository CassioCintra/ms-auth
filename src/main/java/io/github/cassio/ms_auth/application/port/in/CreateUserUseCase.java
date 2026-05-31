package io.github.cassio.ms_auth.application.port.in;

public interface CreateUserUseCase {

    record CreateUserCommand(
        String username,
        String email,
        String password,
        String fullName
    ) {}

    record UserCreatedView(String username, String email) {}

    UserCreatedView create(CreateUserCommand command);
}
