package io.github.cassio.ms_auth.domain.user;

import java.util.List;

public record AuthUser(
    String username,
    String password,
    String email,
    String fullName,
    boolean enabled,
    List<String> authorities
) {}
