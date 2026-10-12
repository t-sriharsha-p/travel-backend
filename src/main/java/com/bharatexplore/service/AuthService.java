package com.bharatexplore.service;

import com.bharatexplore.dto.AuthDtos.*;
import com.bharatexplore.exception.ApiException;
import com.bharatexplore.model.User;
import com.bharatexplore.repository.UserRepository;
import com.bharatexplore.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        users.save(user);
        return new AuthResponse(jwt.generateToken(email), toDto(user));
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generateToken(user.getEmail()), toDto(user));
    }

    public User requireUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account not found. Please log in again"));
    }

    public UserDto me(String email) {
        return toDto(requireUser(email));
    }

    private UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail());
    }
}
