package com.newwork.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity()
public class SecurityConfig {
    private final JwtAuthFilter jwt;
    public SecurityConfig(JwtAuthFilter jwt){ this.jwt = jwt; }

    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());
        http.authorizeHttpRequests(reg -> reg
                .requestMatchers("/healthz", "/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll());
        http.addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
