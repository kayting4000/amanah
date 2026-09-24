package com.amanah.banking.dto.request;

import jakarta.validation.constraints.NotNull;

public class CreateAccountRequest {
    @NotNull
    private String accountType; // SAVINGS or WADIAH

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}
