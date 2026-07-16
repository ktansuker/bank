package com.example.bank.dto;

import com.example.bank.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ExchangeRequest {

    @NotNull(message = "Kaynak para birimi seçilmelidir")
    private Currency fromCurrency;

    @NotNull(message = "Hedef para birimi seçilmelidir")
    private Currency toCurrency;

    @NotNull(message = "Miktar boş olamaz")
    @DecimalMin(value = "0.01", message = "Miktar 0'dan büyük olmalıdır")
    private BigDecimal amount;

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(Currency fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(Currency toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
