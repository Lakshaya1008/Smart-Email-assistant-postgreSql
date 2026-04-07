package com.email.writer.service;

import com.email.writer.dto.AuthResponse;
import com.email.writer.dto.RegisterRequest;
import com.email.writer.entity.User;
import com.email.writer.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock private UserService         userService;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtUtil             jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private User            savedUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("password123");
        validRequest.setFirstName("Test");
        validRequest.setLastName("User");

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setFirstName("Test");
        savedUser.setLastName("User");
        savedUser.setIsActive(true);
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — happy path returns AuthResponse with token")
    void register_happyPath_returnsAuthResponse() {
        when(userService.existsByUsername("testuser")).thenReturn(false);
        when(userService.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userService.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser)).thenReturn("jwt.token.here");

        AuthResponse response = authService.register(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getLastName()).isEqualTo("User");
        assertThat(response.getId()).isEqualTo(1L);

        verify(userService).save(any(User.class));
        verify(passwordEncoder).encode("password123");
        verify(jwtUtil).generateToken(savedUser);
    }

    @Test
    @DisplayName("register — duplicate username throws RuntimeException")
    void register_duplicateUsername_throwsException() {
        when(userService.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(userService, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("register — duplicate email throws RuntimeException")
    void register_duplicateEmail_throwsException() {
        when(userService.existsByUsername("testuser")).thenReturn(false);
        when(userService.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");

        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("register — password is encoded, not stored in plain text")
    void register_passwordIsEncoded() {
        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // Verify the saved user has an encoded password, not the raw one
            assertThat(u.getPassword()).isEqualTo("$2a$10$hashed");
            assertThat(u.getPassword()).doesNotContain("password123");
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.register(validRequest);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register — firstName and lastName are persisted")
    void register_firstNameLastName_arePersisted() {
        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            assertThat(u.getFirstName()).isEqualTo("Test");
            assertThat(u.getLastName()).isEqualTo("User");
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.register(validRequest);

        verify(userService).save(argThat(u ->
                "Test".equals(u.getFirstName()) && "User".equals(u.getLastName())
        ));
    }

    // ── getCurrentUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentUser — null authentication throws RuntimeException")
    void getCurrentUser_nullAuth_throwsException() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not authenticated");
    }
}