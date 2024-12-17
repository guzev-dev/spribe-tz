package com.spribe.tz.service.impl;

import com.spribe.tz.controller.dto.CurrencyRateDTO;
import com.spribe.tz.controller.dto.RateDTO;
import com.spribe.tz.dao.CurrencyDao;
import com.spribe.tz.dao.entity.Currency;
import com.spribe.tz.dao.entity.CurrencyLog;
import com.spribe.tz.dao.CurrencyLogDao;
import com.spribe.tz.service.CurrencyRateService;
import com.spribe.tz.service.ExchangeRateIntegrationService;
import com.spribe.tz.service.impl.model.CurrencyExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final Map<String, List<CurrencyExchangeRate>> currencyRates = new HashMap<>();

    private final CurrencyDao currencyDao;
    private final CurrencyLogDao currencyLogDao;
    private final ExchangeRateIntegrationService exchangeRateIntegrationService;

    @Override
    public void addCurrency(final String currencyCode) {
        currencyDao.save(new Currency(currencyCode));

        refreshCurrencyRates(getCurrenciesCodes());
    }

    @Override
    public List<String> getUsedCurrencies() {
        return getCurrenciesCodes().stream()
                          .sorted()
                          .toList();
    }

    @Override
    public CurrencyRateDTO getCurrencyRates(final String currencyCode) {
        Optional.ofNullable(currencyCode)
                .filter(not(currencyDao::checkIfExistsByCurrencyCode))
                .ifPresent(code -> {
                    throw new RuntimeException("""
                        Currency '%s' is not available for getting exchange rates.
                        Please try to add currencyCode using existing API.
                        """.formatted(currencyCode));
                });

        if (currencyRates.isEmpty()) {
            refreshCurrencyRates(getCurrenciesCodes());
        }

        return Optional.ofNullable(currencyRates.get(currencyCode))
                       .map(rates -> {
                           Map<String, RateDTO> ratesMap = rates.stream()
                                                                .collect(Collectors.toMap(CurrencyExchangeRate::anotherCurrency,
                                                                    rate -> RateDTO.builder()
                                                                                   .rate(rate.rate())
                                                                                   .doubleConversion(rate.doubleConversion())
                                                                                   .build()));

                           return CurrencyRateDTO.builder()
                                                 .baseCurrency(currencyCode)
                                                 .rates(ratesMap)
                                                 .build();
                       })
                       .orElseThrow();
    }

    @Scheduled(fixedDelayString = "${currencyRate.fetch.frequency}", timeUnit = TimeUnit.SECONDS)
    private void receiveExchangeRates() {
        Map<String, List<CurrencyExchangeRate>> rates = refreshCurrencyRates(getCurrenciesCodes());

        rates.forEach((currency, exchangeRates) ->
            currencyLogDao.saveAll(
                exchangeRates.stream()
                             .map(rate -> CurrencyLog.builder()
                                                     .baseCurrencyCode(currency)
                                                     .anotherCurrencyCode(rate.anotherCurrency())
                                                     .rate(rate.rate())
                                                     .rateDateTime(rate.dateTime())
                                                     .doubleConversion(rate.doubleConversion())
                                                     .build())
                             .toList()));
    }

    private Map<String, List<CurrencyExchangeRate>> refreshCurrencyRates(final Collection<String> currenciesCodes) {
        Map<String, List<CurrencyExchangeRate>> rates = exchangeRateIntegrationService.getExchangeRateForCurrencies(currenciesCodes);
        currencyRates.putAll(rates);

        return rates;
    }

    private List<String> getCurrenciesCodes() {
        return currencyDao.getCurrencies().stream()
                          .map(Currency::getCurrencyCode)
                          .toList();
    }

}
