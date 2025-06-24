package com.ecommerce.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ecommerce.user.security.RateLimitingFilter;
import com.ecommerce.user.security.IpWhitelistFilter;
import com.ecommerce.user.security.SecurityAuditFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test") // Don't load this config in test profile to avoid bean conflicts
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/auth/login", "/api/auth/register")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(3) // Maximum 3 concurrent sessions per user
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry())
                .and()
                .sessionFixation().migrateSession()
                .invalidSessionUrl("/api/auth/session-expired")
            )
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
            )
            .headers(headers -> headers
                .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "strict-origin-when-cross-origin"))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", 
                    "camera=(), microphone=(), geolocation=(), interest-cohort=()"))
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/forgot-password").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        jwtConverter.setPrincipalClaimName("sub");
        return jwtConverter;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            String body = "{\"error\":\"Unauthorized\",\"message\":\"" + 
                authException.getMessage() + "\",\"timestamp\":\"" + 
                Instant.now().toString() + "\"}";
            
            response.getOutputStream().println(body);
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            String body = "{\"error\":\"Access Denied\",\"message\":\"" + 
                accessDeniedException.getMessage() + "\",\"timestamp\":\"" + 
                Instant.now().toString() + "\"}";
            
            response.getOutputStream().println(body);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.cors.enabled", havingValue = "true", matchIfMissing = true)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.yourdomain.com", "https://localhost:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public FilterRegistrationBean<SecurityAuditFilter> securityAuditFilterRegistration(
            SecurityAuditFilter securityAuditFilter) {
        FilterRegistrationBean<SecurityAuditFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(securityAuditFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // First filter
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            RateLimitingFilter rateLimitingFilter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitingFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2); // Second filter
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IpWhitelistFilter> ipWhitelistFilterRegistration(
            IpWhitelistFilter ipWhitelistFilter) {
        FilterRegistrationBean<IpWhitelistFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(ipWhitelistFilter);
        registration.addUrlPatterns("/api/admin/*", "/actuator/*");
        registration.setOrder(3); // Third filter
        return registration;
    }

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return HeaderHttpSessionIdResolver.xAuthToken();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}