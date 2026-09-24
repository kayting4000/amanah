package com.amanah.banking.controller;

import com.amanah.banking.dto.request.CreateAccountRequest;
import com.amanah.banking.dto.request.DepositRequest;
import com.amanah.banking.dto.request.WithdrawRequest;
import com.amanah.banking.dto.response.AccountResponse;
import com.amanah.banking.dto.response.ApiResponse;
import com.amanah.banking.dto.response.TransactionResponse;
import com.amanah.banking.service.AccountService;
import com.amanah.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody CreateAccountRequest req) {
        AccountResponse response = accountService.createAccount(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Account created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
        @AuthenticationPrincipal UserDetails userDetails) {
        List<AccountResponse> accounts = accountService.getMyAccounts(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Accounts retrieved", accounts));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long accountId) {
        AccountResponse response = accountService.getAccount(userDetails.getUsername(), accountId);
        return ResponseEntity.ok(ApiResponse.ok("Account retrieved", response));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long accountId) {
        BigDecimal balance = accountService.getBalance(userDetails.getUsername(), accountId);
        return ResponseEntity.ok(ApiResponse.ok("Balance retrieved", balance));
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long accountId,
        @Valid @RequestBody DepositRequest req) {
        TransactionResponse response = transactionService.deposit(userDetails.getUsername(), accountId, req);
        return ResponseEntity.ok(ApiResponse.ok("Deposit successful", response));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long accountId,
        @Valid @RequestBody WithdrawRequest req) {
        TransactionResponse response = transactionService.withdraw(userDetails.getUsername(), accountId, req);
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal successful", response));
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long accountId) {
        List<TransactionResponse> history = transactionService.getHistory(userDetails.getUsername(), accountId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction history retrieved", history));
    }
}
