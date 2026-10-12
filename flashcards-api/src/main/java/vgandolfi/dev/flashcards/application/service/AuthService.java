package vgandolfi.dev.flashcards.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.request.LoginRequest;
import vgandolfi.dev.flashcards.application.dto.request.RefreshTokenRequest;
import vgandolfi.dev.flashcards.application.dto.response.LoginResponse;
import vgandolfi.dev.flashcards.application.dto.response.UserResponse;
import vgandolfi.dev.flashcards.domain.model.User;
import vgandolfi.dev.flashcards.domain.repository.UserRepository;
import vgandolfi.dev.flashcards.infrastructure.config.AppProperties;
import vgandolfi.dev.flashcards.infrastructure.security.JwtService;

import java.util.UUID;

/** Autenticacao JWT stateless (login + refresh). */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }
        log.info("auth_login_success userId={}", user.getId());
        return buildLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshTokenRequest request) {
        UUID userId = jwtService.parseRefreshToken(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return LoginResponse.of(accessToken, refreshToken, appProperties.jwt().expiration(), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
