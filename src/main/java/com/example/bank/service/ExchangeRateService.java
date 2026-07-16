package com.example.bank.service;

import com.example.bank.entity.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class ExchangeRateService {

    private static final Map<Currency, BigDecimal> RATE_TO_TRY = Map.of(
            Currency.TRY, BigDecimal.ONE,
            Currency.USD, new BigDecimal("45.00"),
            Currency.EUR, new BigDecimal("54.00"));

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (from == to) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amountInTry = amount.multiply(RATE_TO_TRY.get(from));
        return amountInTry.divide(RATE_TO_TRY.get(to), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        return RATE_TO_TRY.get(from).divide(RATE_TO_TRY.get(to), 6, RoundingMode.HALF_UP);
    }
}
