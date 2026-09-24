package com.amanah.banking.service;

import com.amanah.banking.exception.ResourceNotFoundException;
import com.amanah.banking.model.Account;
import com.amanah.banking.model.User;
import com.amanah.banking.repository.AccountRepository;
import com.amanah.banking.repository.CustomerRepository;
import com.amanah.banking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountRepository accountRepository;

    @InjectMocks private AdminService adminService;

    @Test
    void suspendUser_success() {
        User user = new User();
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        adminService.suspendUser(7L);

        verify(userRepository).updateStatus(7L, User.UserStatus.SUSPENDED);
    }

    @Test
    void suspendUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.suspendUser(99L));
        verify(userRepository, never()).updateStatus(anyLong(), any());
    }

    @Test
    void freezeAccount_success() {
        Account account = new Account();
        account.setId(3L);
        account.setAccountNumber("AMN000000003");
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setBalance(java.math.BigDecimal.ZERO);
        account.setAccountType(Account.AccountType.SAVINGS);

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));

        var response = adminService.freezeAccount(3L);

        assertEquals("FROZEN", response.getStatus());
        verify(accountRepository).updateStatus(3L, Account.AccountStatus.FROZEN);
    }

    @Test
    void closeAccount_notFound_throws() {
        when(accountRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.closeAccount(55L));
    }
}