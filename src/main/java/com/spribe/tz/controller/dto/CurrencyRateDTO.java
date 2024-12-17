package com.spribe.tz.controller.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record CurrencyRateDTO(
    String baseCurrency,
    Map<String, RateDTO> rates
) {}
