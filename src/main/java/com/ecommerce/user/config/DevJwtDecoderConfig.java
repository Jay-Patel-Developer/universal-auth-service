package com.ecommerce.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Configuration
@Profile("development")
public class DevJwtDecoderConfig {
    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> new Jwt(
            token,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Collections.singletonMap("sub", "dummy")
        );
    }
}
