package com.openclassrooms.etudiant.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "ZXR1ZGlhbnQtYmFja2VuZC1kZWZhdWx0LXNlY3JldC1mb3ItaHMyNTYtMzItYnl0ZXM=";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void generate_token_returns_non_empty_jwt() {
        UserDetails userDetails = createUserDetails("agent");

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
    }

    @Test
    void extract_username_returns_token_subject() {
        UserDetails userDetails = createUserDetails("agent");
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("agent");
    }

    @Test
    void is_token_valid_returns_true_for_matching_user() {
        UserDetails userDetails = createUserDetails("agent");
        String token = jwtService.generateToken(userDetails);

        boolean result = jwtService.isTokenValid(token, userDetails);

        assertThat(result).isTrue();
    }

    @Test
    void is_token_valid_returns_false_for_different_user() {
        UserDetails sourceUser = createUserDetails("agent");
        UserDetails otherUser = createUserDetails("other-agent");
        String token = jwtService.generateToken(sourceUser);

        boolean result = jwtService.isTokenValid(token, otherUser);

        assertThat(result).isFalse();
    }

    @Test
    void extract_username_invalid_token_throws_jwt_exception() {
        Assertions.assertThrows(JwtException.class, () -> jwtService.extractUsername("invalid-token"));
    }

    private UserDetails createUserDetails(String username) {
        return User.builder()
                .username(username)
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }
}
