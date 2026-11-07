package com.unishift.unishift_backend.auth;

import com.unishift.unishift_backend.auth.dto.AuthResponse;
import com.unishift.unishift_backend.auth.dto.LoginRequest;
import com.unishift.unishift_backend.auth.dto.RegisterRequest;
import com.unishift.unishift_backend.auth.dto.UserResponse;
import com.unishift.unishift_backend.common.exception.BadRequestException;
import com.unishift.unishift_backend.common.exception.ConflictException;
import com.unishift.unishift_backend.common.exception.ForbiddenException;
import com.unishift.unishift_backend.common.exception.ResourceNotFoundException;
import com.unishift.unishift_backend.security.JwtService;
import com.unishift.unishift_backend.security.UserPrincipal;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import com.unishift.unishift_backend.user.UserRole;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new BadRequestException("Public registration as ADMIN is strictly prohibited");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered: " + normalizedEmail);
        }

        boolean approved = request.role() == UserRole.STUDENT;

        User user = new User(
            request.name().trim(),
            normalizedEmail,
            passwordEncoder.encode(request.password()),
            request.role(),
            true,
            approved
        );

        User savedUser = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.create(savedUser);
        String token = jwtService.generateToken(principal, Map.of(
            "role", savedUser.getRole().name(),
            "approved", savedUser.isApproved(),
            "userId", savedUser.getId()
        ));

        return new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedEmail));

        if (!user.isActive()) {
            throw new ForbiddenException("Account is deactivated");
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtService.generateToken(principal, Map.of(
            "role", user.getRole().name(),
            "approved", user.isApproved(),
            "userId", user.getId()
        ));

        return new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.fromEntity(user));
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return UserResponse.fromEntity(user);
    }
}
