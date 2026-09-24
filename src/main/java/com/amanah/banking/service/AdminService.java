package com.amanah.banking.service;

import com.amanah.banking.dto.response.AccountResponse;
import com.amanah.banking.dto.response.CustomerResponse;
import com.amanah.banking.exception.ResourceNotFoundException;
import com.amanah.banking.model.Account;
import com.amanah.banking.model.Customer;
import com.amanah.banking.model.User;
import com.amanah.banking.repository.AccountRepository;
import com.amanah.banking.repository.CustomerRepository;
import com.amanah.banking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public AdminService(UserRepository userRepository, CustomerRepository customerRepository,
                        AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void suspendUser(Long userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.updateStatus(userId, User.UserStatus.SUSPENDED);
    }

    @Transactional
    public void activateUser(Long userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.updateStatus(userId, User.UserStatus.ACTIVE);
    }

    @Transactional
    public AccountResponse freezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountRepository.updateStatus(accountId, Account.AccountStatus.FROZEN);
        account.setStatus(Account.AccountStatus.FROZEN);
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountRepository.updateStatus(accountId, Account.AccountStatus.ACTIVE);
        account.setStatus(Account.AccountStatus.ACTIVE);
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse closeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountRepository.updateStatus(accountId, Account.AccountStatus.CLOSED);
        account.setStatus(Account.AccountStatus.CLOSED);
        return AccountResponse.from(account);
    }

    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return CustomerResponse.from(customer);
    }
}
