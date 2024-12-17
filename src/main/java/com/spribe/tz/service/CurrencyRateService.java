package com.spribe.tz.service;

import com.spribe.tz.controller.dto.CurrencyRateDTO;

import java.util.List;

public interface CurrencyRateService {

    void addCurrency(String currencyCode);

    List<String> getUsedCurrencies();

    CurrencyRateDTO getCurrencyRates(String currencyCode);

}
