package com.amanah.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class WithdrawRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "Withdrawal amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Withdrawal amount must have at most 15 integer and 4 fractional digits")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
