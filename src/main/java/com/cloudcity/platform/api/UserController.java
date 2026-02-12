package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.UserCreateRequest;
import com.cloudcity.platform.api.dto.UserResponse;
import com.cloudcity.platform.domain.User;
import com.cloudcity.platform.service.OrgService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Users")
public class UserController {
    private final OrgService orgService;

    public UserController(OrgService orgService) {
        this.orgService = orgService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = orgService.createUser(request);
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
