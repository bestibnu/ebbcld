package com.cloudcity.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

public class OrgCreateRequest {
    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
