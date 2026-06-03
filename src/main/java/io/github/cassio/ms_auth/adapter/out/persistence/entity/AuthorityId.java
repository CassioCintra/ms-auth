package io.github.cassio.ms_auth.adapter.out.persistence.entity;

import java.io.Serializable;

// Substituído por @ElementCollection em UserEntity.
public record AuthorityId(String username, String authority) implements Serializable {}
