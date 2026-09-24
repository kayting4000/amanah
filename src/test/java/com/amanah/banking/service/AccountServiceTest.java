package com.amanah.banking.service;

import com.amanah.banking.dto.request.CreateAccountRequest;
import com.amanah.banking.dto.response.AccountResponse;
import com.amanah.banking.exception.InvalidTransactionException;
import com.amanah.banking.exception.ResourceNotFoundException;
import com.amanah.banking.exception.UnauthorizedAccountAccessException;
import com.amanah.banking.model.Account;
import com.amanah.banking.model.Customer;
import com.amanah.banking.model.User;
import com.amanah.banking.repository.AccountRepository;
import com.amanah.banking.repository.CustomerRepository;
import com.amanah.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AccountService accountService;

    private User user;
    private Customer customer;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("ali");

        customer = new Customer();
        customer.setId(5L);
        customer.setUserId(1L);
        customer.setFirstName("Ali");
        customer.setLastName("Khan");
    }

    @Test
    void createAccount_success() {
        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(100L);

        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType("savings");

        AccountResponse response = accountService.createAccount("ali", req);

        assertNotNull(response);
        assertEquals("SAVINGS", response.getAccountType());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getBalance()));
    }

    @Test
    void createAccount_invalidType_throws() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType("CHECKING");

        assertThrows(InvalidTransactionException.class,
            () -> accountService.createAccount("ali", req));
    }

    @Test
    void getAccount_notOwned_throws() {
        Account account = new Account();
        account.setId(100L);
        account.setCustomerId(999L); // different customer
        account.setStatus(Account.AccountStatus.ACTIVE);

        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));

        assertThrows(UnauthorizedAccountAccessException.class,
            () -> accountService.getAccount("ali", 100L));
    }

    @Test
    void getAccount_notFound_throws() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> accountService.getAccount("ali", 404L));
    }

    @Test
    void getActiveAccount_frozen_throws() {
        Account account = new Account();
        account.setId(100L);
        account.setAccountNumber("AMN000000100");
        account.setStatus(Account.AccountStatus.FROZEN);

        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        assertThrows(com.amanah.banking.exception.AccountNotActiveException.class,
            () -> accountService.getActiveAccountById(100L));
    }
}