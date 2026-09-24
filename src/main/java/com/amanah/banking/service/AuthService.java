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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new DuplicateResourceException("Username already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new DuplicateResourceException("Email already registered");

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);
        Long userId = userRepository.save(user);

        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setFirstName(req.getFirstName());
        customer.setMiddleName(req.getMiddleName());
        customer.setLastName(req.getLastName());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());
        customerRepository.save(customer);

        String token = jwtUtil.generateToken(req.getUsername(), User.Role.CUSTOMER.name());
        return new AuthResponse(token, req.getUsername(), User.Role.CUSTOMER.name());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");

        if (user.getStatus() == User.UserStatus.SUSPENDED)
            throw new BadCredentialsException("Account suspended");

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}
