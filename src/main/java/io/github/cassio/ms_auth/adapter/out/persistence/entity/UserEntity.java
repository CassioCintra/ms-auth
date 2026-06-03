package io.github.cassio.ms_auth.adapter.out.persistence.entity;

import io.github.cassio.ms_auth.domain.user.AuthUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 500)
    private String password;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "authorities",
        joinColumns = @JoinColumn(name = "username", referencedColumnName = "username")
    )
    @Column(name = "authority")
    private List<String> authorities = new ArrayList<>();

    public UserEntity(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    public static UserEntity from(AuthUser domain) {
        UserEntity entity = new UserEntity(
            domain.username(), domain.password(), domain.email(), domain.fullName());
        entity.enabled = domain.enabled();
        entity.authorities = new ArrayList<>(domain.authorities());
        return entity;
    }

    public AuthUser toDomain() {
        return new AuthUser(username, password, email, fullName, enabled, List.copyOf(authorities));
    }
}
