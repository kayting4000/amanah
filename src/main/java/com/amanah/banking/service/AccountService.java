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
import com.amanah.banking.util.AccountNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse createAccount(String username, CreateAccountRequest req) {
        Account.AccountType type;
        try {
            type = Account.AccountType.valueOf(req.getAccountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTransactionException("Invalid account type. Use SAVINGS or WADIAH");
        }

        Customer customer = getCustomerByUsername(username);

        String accountNumber;
        do {
            accountNumber = AccountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        Account account = new Account();
        account.setCustomerId(customer.getId());
        account.setAccountNumber(accountNumber);
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(Account.AccountStatus.ACTIVE);

        Long id = accountRepository.save(account);
        account.setId(id);
        return AccountResponse.from(account);
    }

    public List<AccountResponse> getMyAccounts(String username) {
        Customer customer = getCustomerByUsername(username);
        return accountRepository.findByCustomerId(customer.getId())
            .stream().map(AccountResponse::from).collect(Collectors.toList());
    }

    public AccountResponse getAccount(String username, Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        verifyOwnership(username, account);
        return AccountResponse.from(account);
    }

    public BigDecimal getBalance(String username, Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        verifyOwnership(username, account);
        return account.getBalance();
    }

    public Account getActiveAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new com.amanah.banking.exception.AccountNotActiveException(
                "Account " + account.getAccountNumber() + " is " + account.getStatus());
        return account;
    }

    public Account getActiveAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new com.amanah.banking.exception.AccountNotActiveException(
                "Account " + accountNumber + " is " + account.getStatus());
        return account;
    }

    public void verifyOwnership(String username, Account account) {
        Customer customer = getCustomerByUsername(username);
        if (!account.getCustomerId().equals(customer.getId()))
            throw new UnauthorizedAccountAccessException("You do not own this account");
    }

    private Customer getCustomerByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return customerRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
    }
}
