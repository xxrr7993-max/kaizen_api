package org.rod.kaizen_api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rod.kaizen_api.configs.JwtUtil;
import org.rod.kaizen_api.dtos.auth.LoginRecordDto;
import org.rod.kaizen_api.dtos.auth.RegisterRecordDto;
import org.rod.kaizen_api.exceptions.ConflictException;
import org.rod.kaizen_api.exceptions.InvalidCredentialsException;
import org.rod.kaizen_api.models.RefreshTokenModel;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.repositories.RefreshTokenRepository;
import org.rod.kaizen_api.repositories.UserRepository;
import org.rod.kaizen_api.services.impl.AuthServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtUtil jwtUtil;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthServiceImpl authService;

    @Test
    void register_newEmail_returnsAuthResponse() {
        var dto = new RegisterRecordDto("Test", "test@test.com", "password123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        var user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setName("Test");
        user.setEmail("test@test.com");
        user.setCreatedAt(LocalDateTime.now());
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken(any(), any())).thenReturn("access-token");

        var rt = new RefreshTokenModel();
        rt.setToken("refresh-token");
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any())).thenReturn(rt);

        var result = authService.register(dto);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertNotNull(result.user());
    }

    @Test
    void register_existingEmail_throwsConflict() {
        var dto = new RegisterRecordDto("Test", "existing@test.com", "pass123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);
        assertThrows(ConflictException.class, () -> authService.register(dto));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        var dto = new LoginRecordDto("test@test.com", "wrong");
        var user = new UserModel();
        user.setPassword("hashed");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> authService.login(dto));
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        var dto = new LoginRecordDto("nobody@test.com", "pass");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> authService.login(dto));
    }
}
