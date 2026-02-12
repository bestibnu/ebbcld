package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class OrgMemberCreateRequest {
    @Email
    @NotNull
    private String email;

    @NotNull
    private OrgRole role;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OrgRole getRole() {
        return role;
    }

    public void setRole(OrgRole role) {
        this.role = role;
    }
}
