package com.spribe.tz.service.impl;

import com.spribe.tz.service.ExchangeRateIntegrationService;
import com.spribe.tz.service.impl.model.CurrencyExchangeRate;
import com.spribe.tz.service.impl.model.FixerIoCurrencyExchangeRateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FixerIoExchangeRateService implements ExchangeRateIntegrationService {

    private static final String GET_LATEST_RATES_ENDPOINT = "/latest";

    private final RestTemplate restTemplate;
    private final String host;
    private final String apiKey;
    private final String baseCurrencyCode;

    public FixerIoExchangeRateService(@Value("${integration.exchangeRates.host}") final String host,
                                      @Value("${integration.exchangeRates.apiKey}") final String apiKey,
                                      @Value("${integration.exchangeRates.baseCurrency}") final String baseCurrencyCode,
                                      final RestTemplate restTemplate) {
        this.host = host;
        this.apiKey = apiKey;
        this.baseCurrencyCode = baseCurrencyCode;
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, List<CurrencyExchangeRate>> getExchangeRateForCurrencies(final Collection<String> currenciesCodes) {
        return getExchangeRateForCurrencies(currenciesCodes, baseCurrencyCode);
    }

    @Override
    public Map<String, List<CurrencyExchangeRate>> getExchangeRateForCurrencies(final Collection<String> currenciesCodes,
                                                                                final String baseCurrencyCode) {
        if (currenciesCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        String url = UriComponentsBuilder.fromHttpUrl(host + GET_LATEST_RATES_ENDPOINT)
                                         .queryParam("access_key", apiKey)
                                         .queryParam("symbols", String.join(",", currenciesCodes))
                                         .queryParam("base", baseCurrencyCode)
                                         .toUriString();

        Optional<FixerIoCurrencyExchangeRateResponse> response = Optional.ofNullable(restTemplate.getForObject(url, FixerIoCurrencyExchangeRateResponse.class));

        if (response.map(FixerIoCurrencyExchangeRateResponse::success)
                    .orElse(false)) {
            return mapSuccessfulResponse(response.get(), currenciesCodes);
        }

        throw new RuntimeException("Unable to fetch currency exchange rates.");
    }

    private static Map<String, List<CurrencyExchangeRate>> mapSuccessfulResponse(final FixerIoCurrencyExchangeRateResponse response,
                                                                                 final Collection<String> currenciesCodes) {
        final int listSize = currenciesCodes.size();
        Map<String, List<CurrencyExchangeRate>> result = currenciesCodes.stream()
                                                                        .collect(Collectors.toMap(Function.identity(), currency -> new ArrayList<>(listSize)));
        Map<String, BigDecimal> fetchedRates = response.rates();
        Instant dateTime = Optional.ofNullable(response.timestamp())
            .map(Instant::ofEpochSecond)
            .orElse(null);
        String baseCurrencyCode = response.base();

        result.forEach((currencyCode, list) -> {
            boolean baseCurrency = currencyCode.equals(baseCurrencyCode);

            fetchedRates.forEach((anotherCurrencyCode, rate) -> {
                if (!currencyCode.equals(anotherCurrencyCode)) {
                    list.add(CurrencyExchangeRate.builder()
                                                 .baseCurrency(currencyCode)
                                                 .anotherCurrency(anotherCurrencyCode)
                                                 .rate(baseCurrency
                                                     ? rate
                                                     : rate.divide(fetchedRates.get(currencyCode), MathContext.DECIMAL64))
                                                 .dateTime(dateTime)
                                                 .doubleConversion(!baseCurrency && !anotherCurrencyCode.equals(baseCurrencyCode))
                                                 .build());
                }
            });
        });

        return result;
    }

}
