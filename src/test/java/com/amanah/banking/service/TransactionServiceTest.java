package com.amanah.banking.service;

import com.amanah.banking.dto.request.DepositRequest;
import com.amanah.banking.dto.request.TransferRequest;
import com.amanah.banking.dto.request.WithdrawRequest;
import com.amanah.banking.dto.response.TransactionResponse;
import com.amanah.banking.exception.InsufficientBalanceException;
import com.amanah.banking.exception.InvalidTransactionException;
import com.amanah.banking.model.Account;
import com.amanah.banking.model.Transaction;
import com.amanah.banking.repository.AccountRepository;
import com.amanah.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountService accountService;

    @InjectMocks private TransactionService transactionService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("AMN000000001");
        account.setBalance(new BigDecimal("1000.0000"));
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setAccountType(Account.AccountType.SAVINGS);
    }

    @Test
    void deposit_success() {
        when(accountService.getActiveAccountById(1L)).thenReturn(account);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.deposit(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(10L);

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("500.00"));

        TransactionResponse response = transactionService.deposit("ali", 1L, req);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("500.00").compareTo(response.getAmount()));
        verify(accountService).verifyOwnership("ali", account);
    }

    @Test
    void deposit_atomicUpdateFails_throws() {
        when(accountService.getActiveAccountById(1L)).thenReturn(account);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.deposit(eq(1L), any(BigDecimal.class))).thenReturn(0);

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("500.00"));

        assertThrows(InvalidTransactionException.class,
            () -> transactionService.deposit("ali", 1L, req));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_success() {
        when(accountService.getActiveAccountById(1L)).thenReturn(account);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.withdraw(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(11L);

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("200.00"));

        TransactionResponse response = transactionService.withdraw("ali", 1L, req);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getAmount()));
    }

    @Test
    void withdraw_insufficientBalance_throws() {
        when(accountService.getActiveAccountById(1L)).thenReturn(account);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.withdraw(eq(1L), any(BigDecimal.class))).thenReturn(0);

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("99999.00"));

        assertThrows(InsufficientBalanceException.class,
            () -> transactionService.withdraw("ali", 1L, req));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_success_createsTwoTransactions() {
        Account destination = new Account();
        destination.setId(2L);
        destination.setAccountNumber("AMN000000002");
        destination.setBalance(new BigDecimal("50.0000"));
        destination.setStatus(Account.AccountStatus.ACTIVE);
        destination.setAccountType(Account.AccountType.WADIAH);

        when(accountService.getActiveAccountByNumber("AMN000000001")).thenReturn(account);
        when(accountService.getActiveAccountByNumber("AMN000000002")).thenReturn(destination);
        when(accountRepository.findByAccountNumberForUpdate("AMN000000001")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumberForUpdate("AMN000000002")).thenReturn(Optional.of(destination));
        when(accountRepository.withdraw(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(accountRepository.deposit(eq(2L), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(1L);

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("AMN000000001");
        req.setDestinationAccountNumber("AMN000000002");
        req.setAmount(new BigDecimal("100.00"));

        List<TransactionResponse> result = transactionService.transfer("ali", req);

        assertEquals(2, result.size());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_sameAccount_throws() {
        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("AMN000000001");
        req.setDestinationAccountNumber("AMN000000001");
        req.setAmount(new BigDecimal("100.00"));

        assertThrows(InvalidTransactionException.class,
            () -> transactionService.transfer("ali", req));
        verify(accountRepository, never()).withdraw(anyLong(), any(BigDecimal.class));
    }

    @Test
    void transfer_insufficientSourceBalance_throws() {
        Account destination = new Account();
        destination.setId(2L);
        destination.setAccountNumber("AMN000000002");
        destination.setBalance(new BigDecimal("50.0000"));
        destination.setStatus(Account.AccountStatus.ACTIVE);

        when(accountService.getActiveAccountByNumber("AMN000000001")).thenReturn(account);
        when(accountService.getActiveAccountByNumber("AMN000000002")).thenReturn(destination);
        when(accountRepository.findByAccountNumberForUpdate("AMN000000001")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumberForUpdate("AMN000000002")).thenReturn(Optional.of(destination));
        when(accountRepository.withdraw(eq(1L), any(BigDecimal.class))).thenReturn(0);

        TransferRequest req = new TransferRequest();
        req.setSourceAccountNumber("AMN000000001");
        req.setDestinationAccountNumber("AMN000000002");
        req.setAmount(new BigDecimal("5000.00"));

        assertThrows(InsufficientBalanceException.class,
            () -> transactionService.transfer("ali", req));
        verify(accountRepository, never()).deposit(anyLong(), any(BigDecimal.class));
    }
}