package io.github.cassio.ms_auth.adapter.in.web.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RegisterClientRequest(
    @NotBlank String clientId,
    @NotBlank String clientSecret,
    @NotEmpty List<String> grantTypes,
    @NotEmpty List<String> scopes,
    List<String> redirectUris
) {}
