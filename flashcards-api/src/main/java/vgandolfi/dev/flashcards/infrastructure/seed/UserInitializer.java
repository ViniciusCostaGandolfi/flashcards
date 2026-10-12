package vgandolfi.dev.flashcards.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.domain.model.User;
import vgandolfi.dev.flashcards.domain.repository.UserRepository;
import vgandolfi.dev.flashcards.infrastructure.config.AppProperties;

/**
 * Cria o usuario inicial a partir do env ({@code app.user.email/password}) em
 * todos os perfis (idempotente). Se o env estiver vazio, apenas loga um warn e
 * segue.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public UserInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.User userProps = appProperties.user();
        if (userProps == null || isBlank(userProps.email()) || isBlank(userProps.password())) {
            log.warn("user_initializer_skip reason=env_not_configured");
            return;
        }
        String email = userProps.email().trim().toLowerCase();
        var existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            syncPassword(existing.get(), userProps.password(), email);
            return;
        }
        User user = new User();
        user.setName(deriveName(email));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(userProps.password()));
        User saved = userRepository.save(user);
        log.info("user_initializer_created userId={} email={}", saved.getId(), email);
    }

    private void syncPassword(User user, String password, String email) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(user);
            log.info("user_initializer_synced userId={} email={}", user.getId(), email);
        } else {
            log.info("user_initializer_skip reason=already_exists email={}", email);
        }
    }

    private String deriveName(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        return local.isBlank() ? "Usuario" : local;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
