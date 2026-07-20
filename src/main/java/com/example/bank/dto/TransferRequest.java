package com.example.bank.dto;

import com.example.bank.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Alıcı (kullanıcı ID / e-posta / kullanıcı adı) boş olamaz")
    private String targetIdentifier;

    @NotNull(message = "Para birimi seçilmelidir")
    private Currency currency;

    @NotNull(message = "Miktar boş olamaz")
    @DecimalMin(value = "0.01", message = "Miktar 0'dan büyük olmalıdır")
    private BigDecimal amount;

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
