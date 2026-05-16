package com.parkease.auth.service;

import com.parkease.auth.dto.request.*;
import com.parkease.auth.dto.response.AuthResponse;
import com.parkease.auth.dto.response.UserResponse;
import com.parkease.auth.entity.*;
import com.parkease.auth.exception.BadCredentialsException;
import com.parkease.auth.exception.EmailAlreadyExistsException;
import com.parkease.auth.exception.InvalidTokenException;
import com.parkease.auth.repository.PasswordResetTokenRepository;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl service;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Sanvi Kumari")
                .email("sanvi@test.com")
                .password("encoded")
                .role(Role.DRIVER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        refreshToken = RefreshToken.builder()
                .token("refresh_123")
                .user(user)
                .build();
    }

    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Sanvi Kumari");
        req.setEmail("sanvi@test.com");
        req.setPassword("test123");
        req.setRole(Role.DRIVER);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("test123")).thenReturn("encoded");
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access_123");
        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn(refreshToken);

        AuthResponse response = service.register(req);

        assertNotNull(response);
        assertEquals("access_123", response.getAccessToken());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(req.getEmail(), req.getFullName());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("sanvi@test.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.register(req));
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest req = new LoginRequest();
        req.setEmail("sanvi@test.com");
        req.setPassword("test123");

        when(userRepository.findByEmail(req.getEmail()))
                .thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString()))
                .thenReturn("access_123");
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(refreshToken);

        AuthResponse response = service.login(req);

        assertNotNull(response);
        assertEquals("access_123", response.getAccessToken());

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldThrowWhenLoginBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("sanvi@test.com");
        req.setPassword("wrong");

        doThrow(new org.springframework.security.authentication.BadCredentialsException("bad"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class,
                () -> service.login(req));
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh_123");

        when(refreshTokenService.validateRefreshToken("refresh_123"))
                .thenReturn(refreshToken);
        when(jwtUtil.generateToken(anyString(), anyString()))
                .thenReturn("new_access");

        AuthResponse response = service.refreshToken(req);

        assertNotNull(response);
        assertEquals("new_access", response.getAccessToken());
    }

    @Test
    void shouldLogoutSuccessfully() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        service.logout(user.getEmail());

        verify(refreshTokenService).deleteByUser(user);
    }

    @Test
    void shouldForgotPasswordSuccessfully() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(user.getEmail());

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        service.forgotPassword(req);

        verify(passwordResetTokenRepository).deleteAllByUser_Id(user.getId());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(user.getEmail()), anyString());
    }

    @Test
    void shouldResetPasswordSuccessfully() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset_123")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset_123");
        req.setNewPassword("newpass");

        when(passwordResetTokenRepository.findByToken("reset_123"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded_new");

        service.resetPassword(req);

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(any());
    }

    @Test
    void shouldThrowWhenResetTokenAlreadyUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset_123")
                .user(user)
                .used(true)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset_123");

        when(passwordResetTokenRepository.findByToken("reset_123"))
                .thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class,
                () -> service.resetPassword(req));
    }

    @Test
    void shouldGetProfileSuccessfully() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserResponse response = service.getProfile(user.getEmail());

        assertNotNull(response);
        assertEquals(user.getEmail(), response.getEmail());
    }

    @Test
    void shouldUpdateProfileSuccessfully() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Updated Name");
        req.setEmail("updated@test.com");
        req.setCurrentPassword("encoded");
        req.setNewPassword("newpass");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.matches("encoded", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded_new");

        UserResponse response = service.updateProfile(user.getEmail(), req);

        assertNotNull(response);
        assertEquals("Updated Name", user.getFullName());
        assertEquals("updated@test.com", user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenUpdateProfileEmailExists() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("existing@test.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> service.updateProfile(user.getEmail(), req));
    }

    @Test
    void shouldThrowWhenUpdateProfileBadPassword() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("newpass");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> service.updateProfile(user.getEmail(), req));
    }

    @Test
    void shouldThrowWhenResetTokenExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset_123")
                .user(user)
                .used(false)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .build();

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset_123");

        when(passwordResetTokenRepository.findByToken("reset_123")).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> service.resetPassword(req));
    }

    @Test
    void shouldThrowWhenLoginDisabledAccount() {
        LoginRequest req = new LoginRequest();
        req.setEmail("sanvi@test.com");
        req.setPassword("test123");

        doThrow(new org.springframework.security.authentication.DisabledException("disabled"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> service.login(req));
    }
}