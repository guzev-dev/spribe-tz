package com.spribe.tz.service;

import com.spribe.tz.service.impl.FixerIoExchangeRateService;
import com.spribe.tz.service.impl.model.CurrencyExchangeRate;
import com.spribe.tz.service.impl.model.FixerIoCurrencyExchangeRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateIntegrationServiceTest {

    private FixerIoExchangeRateService exchangeRateService;

    @Mock
    private RestTemplate restTemplate;

    private static final Long EPOCH_SECONDS = 100_000L;

    @BeforeEach
    void setUp() {
        exchangeRateService = new FixerIoExchangeRateService(
            "http://test-api-host.com",
            "test-api-key",
            "test-currency-code",
            restTemplate);
    }

    @Test
    @DisplayName("Should get exchange rate for currencies")
    void shouldGetExchangeRateForCurrencies() {
        when(restTemplate.getForObject(anyString(), eq(FixerIoCurrencyExchangeRateResponse.class)))
            .thenReturn(buildFixerResponse());

        Map<String, List<CurrencyExchangeRate>> result = exchangeRateService.getExchangeRateForCurrencies(List.of("EUR", "UAH", "USD"), "EUR");

        assertThat(result)
            .hasSize(3)
            .containsOnlyKeys("EUR", "UAH", "USD");

        assertThat(result.get("EUR"))
            .hasSize(2)
            .extracting(
                CurrencyExchangeRate::baseCurrency,
                CurrencyExchangeRate::anotherCurrency,
                CurrencyExchangeRate::dateTime,
                CurrencyExchangeRate::rate,
                CurrencyExchangeRate::doubleConversion)
            .containsExactlyInAnyOrder(
                tuple("EUR", "USD", Instant.ofEpochSecond(EPOCH_SECONDS), new BigDecimal("1.05"), false),
                tuple("EUR", "UAH", Instant.ofEpochSecond(EPOCH_SECONDS), new BigDecimal("43.214"), false));

        assertThat(result.get("UAH"))
            .hasSize(2)
            .extracting(
                CurrencyExchangeRate::baseCurrency,
                CurrencyExchangeRate::anotherCurrency,
                CurrencyExchangeRate::dateTime,
                CurrencyExchangeRate::rate,
                CurrencyExchangeRate::doubleConversion)
            .containsExactlyInAnyOrder(
                tuple("UAH", "USD", Instant.ofEpochSecond(EPOCH_SECONDS), new BigDecimal("0.02429768130698385"), true),
                tuple("UAH", "EUR", Instant.ofEpochSecond(EPOCH_SECONDS), new BigDecimal("0.02314064886379414"), false));

        assertThat(result.get("USD"))
            .hasSize(2)
            .extracting(
                CurrencyExchangeRate::baseCurrency,
                CurrencyExchangeRate::anotherCurrency,
                CurrencyExchangeRate::dateTime,
                CurrencyExchangeRate::rate,
                CurrencyExchangeRate::doubleConversion)
            .containsExactlyInAnyOrder(
                tuple("USD", "UAH", Instant.ofEpochSecond(EPOCH_SECONDS), new BigDecimal("41.15619047619048"), true),
                tuple("USD", "EUR", Instant.ofEpochSecond(EPOCH_SECONDS), new BigDecimal("0.9523809523809524"), false));
    }

    @Test
    @DisplayName("Get exchange rate for currencies should call proper endpoint")
    void getExchangeRateForCurrenciesShouldCallProperEndpoint() {
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        when(restTemplate.getForObject(urlCaptor.capture(), eq(FixerIoCurrencyExchangeRateResponse.class)))
            .thenReturn(buildFixerResponse());

        exchangeRateService.getExchangeRateForCurrencies(List.of("EUR", "UAH", "USD"), "EUR");

        assertThat(urlCaptor.getValue())
            .startsWith("http://test-api-host.com/latest")
            .contains("access_key=test-api-key")
            .contains("symbols=EUR,UAH,USD")
            .contains("base=EUR");

        verify(restTemplate).getForObject(anyString(), eq(FixerIoCurrencyExchangeRateResponse.class));
    }

    @Test
    @DisplayName("Get exchange rate for currencies should return empty map for empty currencies codes")
    void getExchangeRateForCurrenciesShouldReturnEmptyMapForEmptyCurrenciesCodes() {
        assertThat(exchangeRateService.getExchangeRateForCurrencies(Collections.emptyList(), "EUR"))
            .isEmpty();
    }

    @Test
    @DisplayName("Get exchange rate for currencies should throw exception when integration API response is null")
    void getExchangeRateForCurrenciesShouldThrowExceptionWhenIntegrationApiResponseIsNull() {
        when(restTemplate.getForObject(anyString(), eq(FixerIoCurrencyExchangeRateResponse.class)))
            .thenReturn(null);
        List<String> currenciesCodes = List.of("EUR", "UAH", "USD");

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> exchangeRateService.getExchangeRateForCurrencies(currenciesCodes, "EUR"))
            .withMessage("Unable to fetch currency exchange rates.");
    }

    @Test
    @DisplayName("Get exchange rate for currencies should throw exception when integration API response is unsuccessful")
    void getExchangeRateForCurrenciesShouldThrowExceptionWhenIntegrationApiResponseIsUnsuccessful() {
        when(restTemplate.getForObject(anyString(), eq(FixerIoCurrencyExchangeRateResponse.class)))
            .thenReturn(FixerIoCurrencyExchangeRateResponse.builder()
                                                           .success(false)
                                                           .build());
        List<String> currenciesCodes = List.of("EUR", "UAH", "USD");

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> exchangeRateService.getExchangeRateForCurrencies(currenciesCodes, "EUR"))
            .withMessage("Unable to fetch currency exchange rates.");
    }

    @Test
    @DisplayName("Should call get exchange rate method with baseCurrencyCode defined in constructor")
    void shouldCallGetExchangeRateMethodWithBaseCurrencyCodeDefinedInConstructor() {
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        when(restTemplate.getForObject(urlCaptor.capture(), eq(FixerIoCurrencyExchangeRateResponse.class)))
            .thenReturn(buildFixerResponse());

        exchangeRateService.getExchangeRateForCurrencies(List.of("EUR", "UAH", "USD"));

        assertThat(urlCaptor.getValue())
            .startsWith("http://test-api-host.com/latest")
            .contains("base=test-currency-code");
    }

    private static FixerIoCurrencyExchangeRateResponse buildFixerResponse() {
        return FixerIoCurrencyExchangeRateResponse.builder()
                                                  .success(true)
                                                  .timestamp(EPOCH_SECONDS)
                                                  .base("EUR")
                                                  .rates(Map.of(
                                                      "EUR", BigDecimal.ONE,
                                                      "USD", new BigDecimal("1.05"),
                                                      "UAH", new BigDecimal("43.214")))
                                                  .build();
    }
}
