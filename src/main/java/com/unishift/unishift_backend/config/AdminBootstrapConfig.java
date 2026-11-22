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

    @Value("${FIRST_ADMIN_NAME:${ADMIN_NAME:Platform Administrator}}")
    private String adminName;

    @Value("${FIRST_ADMIN_EMAIL:${ADMIN_EMAIL:admin@unishift.com}}")
    private String adminEmail;

    @Value("${FIRST_ADMIN_PASSWORD:${ADMIN_PASSWORD:AdminSecurePassword123!}}")
    private String adminPassword;

    @Bean
    public ApplicationRunner bootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Bootstrap admin@unishift.com
            bootstrapUser(
                userRepository,
                passwordEncoder,
                "admin@unishift.com",
                "AdminSecurePassword123!",
                "Platform Administrator",
                UserRole.ADMIN,
                true
            );

            // Bootstrap admin@example.com
            bootstrapUser(
                userRepository,
                passwordEncoder,
                "admin@example.com",
                "AdminSecurePassword123!",
                "Platform Administrator",
                UserRole.ADMIN,
                true
            );

            // If a custom admin is configured in env, bootstrap that as well
            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
                bootstrapUser(
                    userRepository,
                    passwordEncoder,
                    adminEmail,
                    adminPassword,
                    adminName != null && !adminName.isBlank() ? adminName : "Platform Administrator",
                    UserRole.ADMIN,
                    true
                );
            }

            // Bootstrap demo student (student@example.com)
            bootstrapUser(
                userRepository,
                passwordEncoder,
                "student@example.com",
                "Student1234!",
                "Demo Student",
                UserRole.STUDENT,
                true
            );

            // Bootstrap demo employer (employer@example.com)
            bootstrapUser(
                userRepository,
                passwordEncoder,
                "employer@example.com",
                "Employer1234!",
                "Demo Employer Inc",
                UserRole.EMPLOYER,
                true
            );
        };
    }

    private void bootstrapUser(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        String email,
        String password,
        String name,
        UserRole role,
        boolean approved
    ) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(
            user -> {
                boolean changed = false;
                if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode(password));
                    changed = true;
                }
                if (!user.isActive()) {
                    user.setActive(true);
                    changed = true;
                }
                if (approved && !user.isApproved()) {
                    user.setApproved(true);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(user);
                }
            },
            () -> {
                User user = new User(
                    name,
                    normalizedEmail,
                    passwordEncoder.encode(password),
                    role,
                    true,
                    approved
                );
                userRepository.save(user);
            }
        );
    }
}
