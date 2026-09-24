package com.amanah.banking.service;

import com.amanah.banking.dto.request.LoginRequest;
import com.amanah.banking.dto.request.RegisterRequest;
import com.amanah.banking.dto.response.AuthResponse;
import com.amanah.banking.exception.DuplicateResourceException;
import com.amanah.banking.model.Customer;
import com.amanah.banking.model.User;
import com.amanah.banking.repository.CustomerRepository;
import com.amanah.banking.repository.UserRepository;
import com.amanah.banking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("ali");
        registerRequest.setEmail("ali@amanah.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Ali");
        registerRequest.setLastName("Khan");
        registerRequest.setPhone("1234567");
    }

    @Test
    void register_success_createsUserAndCustomer() {
        when(userRepository.existsByUsername("ali")).thenReturn(false);
        when(userRepository.existsByEmail("ali@amanah.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(1L);
        when(customerRepository.save(any(Customer.class))).thenReturn(1L);
        when(jwtUtil.generateToken("ali", "CUSTOMER")).thenReturn("token-abc");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("token-abc", response.getToken());
        assertEquals("ali", response.getUsername());
        assertEquals("CUSTOMER", response.getRole());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.existsByUsername("ali")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByUsername("ali")).thenReturn(false);
        when(userRepository.existsByEmail("ali@amanah.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
    }

    @Test
    void login_success_returnsToken() {
        User user = new User();
        user.setUsername("ali");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);

        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("ali", "CUSTOMER")).thenReturn("token-xyz");

        LoginRequest req = new LoginRequest();
        req.setUsername("ali");
        req.setPassword("password123");

        AuthResponse response = authService.login(req);

        assertEquals("token-xyz", response.getToken());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setUsername("ali");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);

        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("ali");
        req.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_suspendedUser_throws() {
        User user = new User();
        user.setUsername("ali");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.SUSPENDED);

        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsername("ali");
        req.setPassword("password123");

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}