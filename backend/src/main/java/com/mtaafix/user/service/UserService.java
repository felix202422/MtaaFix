package com.mtaafix.user.service;

import com.mtaafix.common.exceptions.AccountInactiveException;
import com.mtaafix.common.exceptions.InvalidCredentialsException;
import com.mtaafix.common.exceptions.InvalidTokenException;
import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.domain.User.Role;
import com.mtaafix.user.dto.AuthResponse;
import com.mtaafix.user.dto.RegisterRequest;
import com.mtaafix.user.dto.UpdateProfileRequest;
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
                request.mobile(), Role.CITIZEN);
        user = userRepository.save(user);
        return toDto(user);
    }

    public AuthResponse authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        return AuthResponse.of(accessToken, refreshToken, 900L, toDto(user));
    }

    public User refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !tokenProvider.validateToken(refreshToken)
                || !JwtTokenProvider.TYPE_REFRESH.equals(tokenProvider.getType(refreshToken))) {
            throw new InvalidTokenException();
        }
        String subject = tokenProvider.getSubject(refreshToken);
        User user = userRepository.findById(subject)
                .orElseThrow(() -> new InvalidTokenException("Token subject does not exist."));
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        return user;
    }

    public UserDto findById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toDto(user);
    }

    public UserDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return toDto(user);
    }

    public UserDto updateProfile(User user, UpdateProfileRequest request) {
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Full name must not be blank.");
        }
        user.setFullName(request.fullName().trim());
        if (request.mobile() != null) {
            user.setMobile(request.mobile().isBlank() ? null : request.mobile().trim());
        }
        return toDto(userRepository.save(user));
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getMobile(),
                user.getRole(), user.getOrganisationId());
    }
}
