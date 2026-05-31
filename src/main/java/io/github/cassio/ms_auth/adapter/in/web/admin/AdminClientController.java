package io.github.cassio.ms_auth.adapter.in.web.admin;

import io.github.cassio.ms_auth.adapter.in.web.admin.request.RegisterClientRequest;
import io.github.cassio.ms_auth.adapter.in.web.admin.response.ClientResponse;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase.RegisterClientCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/admin/clients")
@RequiredArgsConstructor
public class AdminClientController {

    private final RegisterClientUseCase registerClientUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse register(@Valid @RequestBody RegisterClientRequest request) {
        log.info("POST /auth/admin/clients clientId={}", request.clientId());

        var view = registerClientUseCase.register(new RegisterClientCommand(
            request.clientId(),
            request.clientSecret(),
            request.grantTypes(),
            request.scopes(),
            request.redirectUris()
        ));

        log.debug("Client criado: id={}", view.id());
        return new ClientResponse(view.id(), view.clientId());
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String clientId) {
        log.info("DELETE /auth/admin/clients/{}", clientId);
        registerClientUseCase.revoke(clientId);
    }
}
