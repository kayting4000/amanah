package com.amanah.banking.service;

import com.amanah.banking.model.User;
import com.amanah.banking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void suspendedUserIsDisabledForExistingJwtRequests() {
        User user = user(User.UserStatus.SUSPENDED);
        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));

        UserDetails details = new UserDetailsServiceImpl(userRepository).loadUserByUsername("ali");

        assertFalse(details.isEnabled());
    }

    @Test
    void activeUserIsEnabled() {
        User user = user(User.UserStatus.ACTIVE);
        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));

        UserDetails details = new UserDetailsServiceImpl(userRepository).loadUserByUsername("ali");

        assertTrue(details.isEnabled());
    }

    private User user(User.UserStatus status) {
        User user = new User();
        user.setUsername("ali");
        user.setPasswordHash("hash");
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(status);
        return user;
    }
}