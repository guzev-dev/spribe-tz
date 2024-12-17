package com.spribe.tz.service;

import com.spribe.tz.service.impl.model.CurrencyExchangeRate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExchangeRateIntegrationService {

    Map<String, List<CurrencyExchangeRate>> getExchangeRateForCurrencies(Collection<String> currenciesCodes);

    Map<String, List<CurrencyExchangeRate>> getExchangeRateForCurrencies(Collection<String> currenciesCodes,
                                                                         String baseCurrencyCode);

}
