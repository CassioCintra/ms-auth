package io.github.cassio.ms_auth.domain.user;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("Usuário já existe com o e-mail: " + email);
    }
}
