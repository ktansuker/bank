package com.example.bank.dto;

import com.example.bank.entity.Currency;
import java.math.BigDecimal;

public class WalletDto {
    private Currency currency;
    private BigDecimal balance;

    public WalletDto() {
    }

    public WalletDto(Currency currency, BigDecimal balance) {
        this.currency = currency;
        this.balance = balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
