package com.spribe.tz.service.impl.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record FixerIoCurrencyExchangeRateResponse(
    boolean success,
    Long timestamp,
    String base,
    Map<String, BigDecimal> rates
) {}
