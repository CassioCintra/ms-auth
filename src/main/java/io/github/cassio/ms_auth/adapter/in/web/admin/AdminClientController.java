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
        log.info("Solicitação de registro de novo client OAuth2: clientId={} grantTypes={} scopes={}",
            request.clientId(), request.grantTypes(), request.scopes());

        var view = registerClientUseCase.register(new RegisterClientCommand(
            request.clientId(),
            request.clientSecret(),
            request.grantTypes(),
            request.scopes(),
            request.redirectUris()
        ));

        log.info("Client OAuth2 registrado com sucesso: clientId={} id={}", view.clientId(), view.id());
        return new ClientResponse(view.id(), view.clientId());
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String clientId) {
        log.info("Solicitação de revogação de client OAuth2: clientId={}", clientId);
        registerClientUseCase.revoke(clientId);
        log.info("Client OAuth2 revogado com sucesso: clientId={}", clientId);
    }
}
