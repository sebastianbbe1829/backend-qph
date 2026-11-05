package com.example.demo.config;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || !username.matches("^[a-zA-Z0-9._-]{3,50}$")) {
            throw new IllegalStateException("El usuario inicial debe tener de 3 a 50 caracteres válidos");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("La contraseña inicial debe tener entre 8 y 72 caracteres");
        }
        if (userRepository.count() != 0) {
            throw new IllegalStateException(
                    "El bootstrap inicial solo se permite cuando no existen usuarios; desactívalo");
        }

        userRepository.save(AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role("ADMIN")
                .build());
        logger.info("Se creó el administrador inicial '{}'; desactiva el bootstrap y elimina sus variables", username);
    }
}
