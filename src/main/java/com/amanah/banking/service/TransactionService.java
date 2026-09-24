package com.amanah.banking.service;

import com.amanah.banking.dto.request.DepositRequest;
import com.amanah.banking.dto.request.TransferRequest;
import com.amanah.banking.dto.request.WithdrawRequest;
import com.amanah.banking.dto.response.TransactionResponse;
import com.amanah.banking.exception.InsufficientBalanceException;
import com.amanah.banking.exception.InvalidTransactionException;
import com.amanah.banking.exception.ResourceNotFoundException;
import com.amanah.banking.model.Account;
import com.amanah.banking.model.Transaction;
import com.amanah.banking.repository.AccountRepository;
import com.amanah.banking.repository.TransactionRepository;
import com.amanah.banking.util.ReferenceNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional
    public TransactionResponse deposit(String username, Long accountId, DepositRequest req) {
        Account account = accountService.getActiveAccountById(accountId);
        accountService.verifyOwnership(username, account);

        BigDecimal balanceBefore = account.getBalance();
        int rows = accountRepository.deposit(accountId, req.getAmount());
        if (rows == 0) throw new InvalidTransactionException("Deposit failed");

        BigDecimal balanceAfter = balanceBefore.add(req.getAmount());
        Transaction tx = buildTransaction(
            accountId, null,
            Transaction.TransactionType.DEPOSIT,
            req.getAmount(), balanceBefore, balanceAfter,
            req.getDescription() != null ? req.getDescription() : "Deposit"
        );
        transactionRepository.save(tx);
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse withdraw(String username, Long accountId, WithdrawRequest req) {
        Account account = accountService.getActiveAccountById(accountId);
        accountService.verifyOwnership(username, account);

        BigDecimal balanceBefore = account.getBalance();
        // Atomic SQL: UPDATE ... WHERE balance >= amount
        int rows = accountRepository.withdraw(accountId, req.getAmount());
        if (rows == 0) throw new InsufficientBalanceException("Insufficient balance");

        BigDecimal balanceAfter = balanceBefore.subtract(req.getAmount());
        Transaction tx = buildTransaction(
            accountId, null,
            Transaction.TransactionType.WITHDRAWAL,
            req.getAmount(), balanceBefore, balanceAfter,
            req.getDescription() != null ? req.getDescription() : "Withdrawal"
        );
        transactionRepository.save(tx);
        return TransactionResponse.from(tx);
    }

    /**
     * Transfer flow — all steps run inside one database transaction.
     * If any step fails, Spring rolls back the entire operation.
     * Concurrency: atomic SQL WHERE balance >= amount prevents double-spending.
     */
    @Transactional
    public List<TransactionResponse> transfer(String username, TransferRequest req) {
        if (req.getSourceAccountNumber().equals(req.getDestinationAccountNumber()))
            throw new InvalidTransactionException("Cannot transfer to the same account");

        Account source = accountService.getActiveAccountByNumber(req.getSourceAccountNumber());
        Account destination = accountService.getActiveAccountByNumber(req.getDestinationAccountNumber());

        accountService.verifyOwnership(username, source);

        BigDecimal sourceBefore = source.getBalance();
        // Atomic debit
        int debitRows = accountRepository.withdraw(source.getId(), req.getAmount());
        if (debitRows == 0) throw new InsufficientBalanceException("Insufficient balance for transfer");

        // Re-read destination balance for accurate record
        BigDecimal destBefore = destination.getBalance();
        // Atomic credit
        int creditRows = accountRepository.deposit(destination.getId(), req.getAmount());
        if (creditRows == 0) throw new InvalidTransactionException("Credit to destination failed");

        String sharedRef = ReferenceNumberGenerator.generate();
        String desc = req.getDescription() != null ? req.getDescription() : "Transfer";

        Transaction outTx = buildTransactionWithRef(
            sharedRef, source.getId(), destination.getId(),
            Transaction.TransactionType.TRANSFER_OUT,
            req.getAmount(), sourceBefore, sourceBefore.subtract(req.getAmount()),
            desc + " to " + destination.getAccountNumber()
        );

        Transaction inTx = buildTransactionWithRef(
            ReferenceNumberGenerator.generate(), destination.getId(), source.getId(),
            Transaction.TransactionType.TRANSFER_IN,
            req.getAmount(), destBefore, destBefore.add(req.getAmount()),
            desc + " from " + source.getAccountNumber()
        );

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        return List.of(TransactionResponse.from(outTx), TransactionResponse.from(inTx));
    }

    public List<TransactionResponse> getHistory(String username, Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountService.verifyOwnership(username, account);
        return transactionRepository.findByAccountId(accountId)
            .stream().map(TransactionResponse::from).collect(Collectors.toList());
    }

    public TransactionResponse getTransaction(String username, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        Account account = accountRepository.findById(tx.getAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountService.verifyOwnership(username, account);
        return TransactionResponse.from(tx);
    }

    private Transaction buildTransaction(Long accountId, Long relatedId,
                                          Transaction.TransactionType type,
                                          BigDecimal amount, BigDecimal before, BigDecimal after,
                                          String description) {
        return buildTransactionWithRef(
            ReferenceNumberGenerator.generate(), accountId, relatedId, type, amount, before, after, description);
    }

    private Transaction buildTransactionWithRef(String ref, Long accountId, Long relatedId,
                                                 Transaction.TransactionType type,
                                                 BigDecimal amount, BigDecimal before, BigDecimal after,
                                                 String description) {
        Transaction tx = new Transaction();
        tx.setReferenceNumber(ref);
        tx.setAccountId(accountId);
        tx.setRelatedAccountId(relatedId);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription(description);
        return tx;
    }
}
