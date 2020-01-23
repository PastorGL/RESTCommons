package io.github.pastorgl.rest.entity;

import java.security.Principal;

public class AuthorizedUser implements Principal {
    private String id;
    private String email;
    private Role role;
    private String name;

    public AuthorizedUser() {
    }

    public AuthorizedUser(String id, String email, Role role, String name) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String getName() {
        return name;
    }
}
