package io.github.cassio.ms_auth.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ConfigurationProperties(prefix = "auth.bootstrap")
public record BootstrapProperties(Admin admin, Map<String, MachineClient> machineClients, Map<String, PkceClient> pkceClients) {

    public BootstrapProperties {
        admin = admin == null ? new Admin(null, null) : admin;
        machineClients = machineClients == null ? Map.of() : Map.copyOf(machineClients);
        pkceClients = pkceClients == null ? Map.of() : Map.copyOf(pkceClients);
    }

    public record Admin(String username, String password) {}

    public record MachineClient(String secret, List<String> scopes, UUID workspaceId) {

        public MachineClient {
            scopes = scopes == null ? List.of() : List.copyOf(scopes);
        }
    }

    public record PkceClient(String redirectUri) {}
}
