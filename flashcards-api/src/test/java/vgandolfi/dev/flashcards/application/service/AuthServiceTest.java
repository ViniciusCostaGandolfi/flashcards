package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vgandolfi.dev.flashcards.application.dto.request.LoginRequest;
import vgandolfi.dev.flashcards.application.dto.request.RefreshTokenRequest;
import vgandolfi.dev.flashcards.application.dto.response.LoginResponse;
import vgandolfi.dev.flashcards.domain.model.User;
import vgandolfi.dev.flashcards.domain.repository.UserRepository;
import vgandolfi.dev.flashcards.infrastructure.config.AppProperties;
import vgandolfi.dev.flashcards.infrastructure.security.JwtService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private final AppProperties appProperties = new AppProperties(
            new AppProperties.Jwt("secret-secret-secret-secret-secret-1234", 3600000L, 604800000L),
            new AppProperties.User("a@b.c", "pwd"),
            new AppProperties.Frontend("http://localhost:3000"));

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Vinicius");
        user.setEmail("vinicius@vgandolfi.dev");
        user.setPasswordHash("hash");
        return user;
    }

    @Test
    void loginReturnsTokensForValidCredentials() {
        User user = user();
        when(userRepository.findByEmail("vinicius@vgandolfi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        // appProperties injected via @InjectMocks needs explicit construction wiring:
        // recreate the service with the real AppProperties.
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, appProperties);

        LoginResponse response = service.login(new LoginRequest("vinicius@vgandolfi.dev", "admin123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600000L);
        assertThat(response.user().email()).isEqualTo("vinicius@vgandolfi.dev");
    }

    @Test
    void loginNormalizesEmailCase() {
        User user = user();
        when(userRepository.findByEmail("vinicius@vgandolfi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, appProperties);

        LoginResponse response = service.login(new LoginRequest("  Vinicius@Vgandolfi.dev ", "admin123"));

        assertThat(response.user().id()).isEqualTo(user.getId());
    }

    @Test
    void loginWithUnknownEmailThrows() {
        when(userRepository.findByEmail("x@y.z")).thenReturn(Optional.empty());
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, appProperties);

        assertThatThrownBy(() -> service.login(new LoginRequest("x@y.z", "pwd")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginWithWrongPasswordThrows() {
        User user = user();
        when(userRepository.findByEmail("vinicius@vgandolfi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, appProperties);

        assertThatThrownBy(() -> service.login(new LoginRequest("vinicius@vgandolfi.dev", "errada")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshIssuesNewTokens() {
        User user = user();
        when(jwtService.parseRefreshToken("refresh")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, appProperties);

        LoginResponse response = service.refresh(new RefreshTokenRequest("refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refreshWithUnknownUserThrows() {
        UUID userId = UUID.randomUUID();
        when(jwtService.parseRefreshToken("refresh")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, appProperties);

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("refresh")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
