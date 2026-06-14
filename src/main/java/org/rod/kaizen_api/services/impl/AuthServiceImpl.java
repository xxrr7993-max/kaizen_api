package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.configs.JwtUtil;
import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.dtos.auth.*;
import org.rod.kaizen_api.exceptions.*;
import org.rod.kaizen_api.models.RefreshTokenModel;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.repositories.RefreshTokenRepository;
import org.rod.kaizen_api.repositories.UserRepository;
import org.rod.kaizen_api.services.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRecordDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("Email already registered");
        }
        UserModel user = new UserModel();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponseDto login(LoginRecordDto dto) {
        UserModel user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto refresh(RefreshRecordDto dto) {
        RefreshTokenModel token = refreshTokenRepository.findByToken(dto.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token expired");
        }
        refreshTokenRepository.delete(token);
        return buildAuthResponse(token.getUser());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRecordDto dto) {
        userRepository.findByEmail(dto.email()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            // In production: send email with resetToken
        });
        // Always return 200 to avoid user enumeration
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRecordDto dto) {
        UserModel user = userRepository.findByPasswordResetToken(dto.token())
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired reset token"));
        if (user.getPasswordResetTokenExpiry() == null ||
                user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Reset token expired");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRecordDto dto, String userId) {
        UserModel user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }

    private AuthResponseDto buildAuthResponse(UserModel user) {
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getUserId());
        String refreshToken = createRefreshToken(user);
        UserProfileDto profile = new UserProfileDto(
                user.getUserId(), user.getName(), user.getEmail(),
                user.getCreatedAt(), user.getStreakCurrent(), user.getStreakRecord()
        );
        return new AuthResponseDto(accessToken, refreshToken, profile);
    }

    private String createRefreshToken(UserModel user) {
        RefreshTokenModel token = new RefreshTokenModel();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        return refreshTokenRepository.save(token).getToken();
    }
}
