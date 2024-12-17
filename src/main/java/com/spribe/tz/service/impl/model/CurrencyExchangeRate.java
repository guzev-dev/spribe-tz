package com.spribe.tz.service.impl.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CurrencyExchangeRate(
    String baseCurrency,
    String anotherCurrency,
    Instant dateTime,
    BigDecimal rate,
    boolean doubleConversion
) {}
