package com.stockmentor.auth.service;

import com.stockmentor.auth.dto.LoginRequest;
import com.stockmentor.auth.dto.RegisterRequest;
import com.stockmentor.auth.validation.PasswordPolicy;
import com.stockmentor.auth.vo.AuthResponse;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.infrastructure.security.JwtTokenProvider;
import com.stockmentor.user.domain.UserRole;
import com.stockmentor.user.domain.UserStatus;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.repository.UserRepository;
import com.stockmentor.user.service.NicknameNormalizer;
import com.stockmentor.user.vo.CurrentUserResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final NicknameNormalizer nicknameNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationService(
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            NicknameNormalizer nicknameNormalizer,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.nicknameNormalizer = nicknameNormalizer;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        requireBcryptCompatibility(
                request.password(),
                ErrorCode.VALIDATION_FAILED
        );
        String normalizedEmail = emailNormalizer.normalize(request.email());
        String normalizedNickname =
                nicknameNormalizer.normalize(request.nickname());
        if (userRepository.findByNormalizedEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Instant authenticationAt = Instant.now();
        LocalDateTime loginAt = LocalDateTime.ofInstant(
                authenticationAt,
                ZoneId.systemDefault()
        );
        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setNickname(normalizedNickname);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(loginAt);

        UserEntity savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
        if (savedUser == null || savedUser.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        if (!userRepository.updateLastLoginAt(savedUser.getId(), loginAt)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        savedUser.setLastLoginAt(loginAt);

        return issueAuthentication(savedUser, authenticationAt);
    }

    public AuthResponse login(LoginRequest request) {
        requireBcryptCompatibility(
                request.password(),
                ErrorCode.AUTH_INVALID_CREDENTIALS
        );
        String normalizedEmail = emailNormalizer.normalize(request.email());
        UserEntity user = userRepository.findByNormalizedEmail(normalizedEmail)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.getStatus() != UserStatus.ACTIVE
                || !passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                )) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        Instant authenticationAt = Instant.now();
        LocalDateTime loginAt = LocalDateTime.ofInstant(
                authenticationAt,
                ZoneId.systemDefault()
        );
        if (!userRepository.updateLastLoginAt(user.getId(), loginAt)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        user.setLastLoginAt(loginAt);

        return issueAuthentication(user, authenticationAt);
    }

    private AuthResponse issueAuthentication(
            UserEntity user,
            Instant issuedAt
    ) {
        String token = jwtTokenProvider.issue(user.getId(), issuedAt);
        return AuthResponse.bearer(
                token,
                jwtTokenProvider.expirationSeconds(),
                toCurrentUser(user)
        );
    }

    private void requireBcryptCompatibility(
            String password,
            ErrorCode errorCode
    ) {
        if (!PasswordPolicy.isBcryptCompatible(password)) {
            throw new BusinessException(errorCode);
        }
    }

    private CurrentUserResponse toCurrentUser(UserEntity user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
