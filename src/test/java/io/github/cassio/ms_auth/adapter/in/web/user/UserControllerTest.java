package io.github.cassio.ms_auth.adapter.in.web.user;

import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase;
import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase.CreateUserCommand;
import io.github.cassio.ms_auth.application.port.in.CreateUserUseCase.UserCreatedView;
import io.github.cassio.ms_auth.domain.user.UserAlreadyExistsException;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean CreateUserUseCase createUserUseCase;
    @MockitoBean JdbcTemplate jdbcTemplate;

    private static final String VALID_BODY = """
        {"username":"john","email":"john@example.com","password":"secret123","fullName":"John Doe"}
        """;

    @Test
    void shouldReturn201WhenRegisteringNewUser() throws Exception {
        given(createUserUseCase.create(any(CreateUserCommand.class)))
            .willReturn(new UserCreatedView("john", "john@example.com"));

        mvc.perform(post("/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("john"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        given(createUserUseCase.create(any(CreateUserCommand.class)))
            .willThrow(new UserAlreadyExistsException("john@example.com"));

        mvc.perform(post("/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
        mvc.perform(post("/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"","email":"not-an-email","password":"short","fullName":""}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_users:admin")
    void shouldReturn201WhenCreatingUserFromInvite() throws Exception {
        given(createUserUseCase.create(any(CreateUserCommand.class)))
            .willReturn(new UserCreatedView("john", "john@example.com"));

        mvc.perform(post("/auth/users").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void shouldReturn401WhenCreatingUserFromInviteWithoutAuth() throws Exception {
        mvc.perform(post("/auth/users").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isUnauthorized());
    }
}
