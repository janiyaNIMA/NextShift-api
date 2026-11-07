package com.unishift.unishift_backend.config;

import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import com.unishift.unishift_backend.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Value("${FIRST_ADMIN_NAME:${app.admin.name:Platform Administrator}}")
    private String adminName;

    @Value("${FIRST_ADMIN_EMAIL:${app.admin.email:}}")
    private String adminEmail;

    @Value("${FIRST_ADMIN_PASSWORD:${app.admin.password:}}")
    private String adminPassword;

    @Bean
    public ApplicationRunner bootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                return;
            }

            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = new User(
                    adminName != null && !adminName.isBlank() ? adminName : "Platform Administrator",
                    adminEmail.trim().toLowerCase(),
                    passwordEncoder.encode(adminPassword),
                    UserRole.ADMIN,
                    true,
                    true
                );
                userRepository.save(admin);
            }
        };
    }
}
