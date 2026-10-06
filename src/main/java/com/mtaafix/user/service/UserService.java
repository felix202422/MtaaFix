package com.mtaafix.user.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.dto.AuthResponse;
import com.mtaafix.user.dto.RegisterRequest;
import com.mtaafix.user.dto.UserDto;
import com.mtaafix.user.repository.UserRepository;
import com.mtaafix.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public UserService(UserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        String passwordHash = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        User user = new User(request.email(), passwordHash, request.fullName(),
                request.mobile(), request.role());
        user = userRepository.save(user);
        return toDto(user);
    }

    public UserDto authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is inactive.");
        }
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        return UserDto.from(toDto(user).user());
    }

    public UserDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return toDto(user);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getMobile(),
                user.getRole(), user.getOrganisationId());
    }
}
