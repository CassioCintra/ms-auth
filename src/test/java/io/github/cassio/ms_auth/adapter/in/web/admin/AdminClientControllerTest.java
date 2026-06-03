package io.github.cassio.ms_auth.adapter.in.web.admin;

import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase.RegisterClientCommand;
import io.github.cassio.ms_auth.application.port.in.RegisterClientUseCase.RegisteredClientView;
import io.github.cassio.ms_auth.domain.client.ClientNotFoundException;
import io.github.cassio.ms_auth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminClientController.class)
@Import(SecurityConfig.class)
class AdminClientControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean RegisterClientUseCase registerClientUseCase;
    @MockitoBean JdbcTemplate jdbcTemplate;

    private static final String VALID_BODY = """
        {"clientId":"svc-x","clientSecret":"secret","grantTypes":["client_credentials"],"scopes":["flags:read"]}
        """;

    @Test
    @WithMockUser(authorities = "ROLE_AUTH_ADMIN")
    void shouldReturn201WhenRegisteringClient() throws Exception {
        given(registerClientUseCase.register(any(RegisterClientCommand.class)))
            .willReturn(new RegisteredClientView("uuid-1", "svc-x"));

        mvc.perform(post("/auth/admin/clients").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("uuid-1"))
            .andExpect(jsonPath("$.clientId").value("svc-x"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_AUTH_ADMIN")
    void shouldReturn400WhenRegisteringClientWithInvalidBody() throws Exception {
        mvc.perform(post("/auth/admin/clients").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"clientId":"","clientSecret":"","grantTypes":[],"scopes":[]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_AUTH_ADMIN")
    void shouldReturn204WhenRevokingExistingClient() throws Exception {
        willDoNothing().given(registerClientUseCase).revoke("svc-x");

        mvc.perform(delete("/auth/admin/clients/svc-x").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "ROLE_AUTH_ADMIN")
    void shouldReturn404WhenRevokingNonExistentClient() throws Exception {
        willThrow(new ClientNotFoundException("ghost")).given(registerClientUseCase).revoke("ghost");

        mvc.perform(delete("/auth/admin/clients/ghost").with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenRegisteringClientWithoutAuth() throws Exception {
        mvc.perform(post("/auth/admin/clients").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isUnauthorized());
    }
}
