package io.github.cassio.ms_auth.infra;

import io.github.cassio.ms_auth.application.port.out.UserRepository;
import io.github.cassio.ms_auth.domain.user.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder {

    static final String ADMIN_ROLE = "ROLE_AUTH_ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        var admin = properties.admin();
        if (isBlank(admin.username()) || isBlank(admin.password())) {
            log.info("Bootstrap admin not configured, skipping");
            return;
        }

        var password = passwordEncoder.encode(admin.password());
        userRepository.findByUsername(admin.username()).ifPresentOrElse(
                existing -> {
                    var authorities = new LinkedHashSet<>(existing.authorities());
                    authorities.add(ADMIN_ROLE);
                    userRepository.update(new AuthUser(existing.username(), password, existing.email(),
                            existing.fullName(), true, List.copyOf(authorities)));
                    log.info("Bootstrap admin updated: username={}", admin.username());
                },
                () -> {
                    userRepository.save(new AuthUser(admin.username(), password, null, null, true,
                            List.of("ROLE_USER", ADMIN_ROLE)));
                    log.info("Bootstrap admin created: username={}", admin.username());
                });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
