package com.cloudcity.platform.security;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    public Optional<String> getSubject(String token) {
        return Optional.empty();
    }

    public boolean isValid(String token) {
        return false;
    }
}
