package com.spribe.tz.controller.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RateDTO(
    BigDecimal rate,
    boolean doubleConversion
) {}
