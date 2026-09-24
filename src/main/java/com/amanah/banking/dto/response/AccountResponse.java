package com.amanah.banking.dto.response;

import com.amanah.banking.model.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;

    public static AccountResponse from(Account a) {
        AccountResponse r = new AccountResponse();
        r.id = a.getId();
        r.accountNumber = a.getAccountNumber();
        r.accountType = a.getAccountType().name();
        r.balance = a.getBalance();
        r.status = a.getStatus().name();
        r.createdAt = a.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
