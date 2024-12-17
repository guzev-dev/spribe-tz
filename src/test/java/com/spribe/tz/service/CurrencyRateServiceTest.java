package com.spribe.tz.service;

import com.spribe.tz.controller.dto.CurrencyRateDTO;
import com.spribe.tz.controller.dto.RateDTO;
import com.spribe.tz.dao.CurrencyDao;
import com.spribe.tz.dao.CurrencyLogDao;
import com.spribe.tz.dao.entity.Currency;
import com.spribe.tz.dao.entity.CurrencyLog;
import com.spribe.tz.service.impl.CurrencyRateServiceImpl;
import com.spribe.tz.service.impl.model.CurrencyExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceTest {

    @InjectMocks
    private CurrencyRateServiceImpl currencyRateService;

    @Mock
    private CurrencyDao currencyDao;

    @Mock
    private CurrencyLogDao currencyLogDao;

    @Mock
    private ExchangeRateIntegrationService exchangeRateIntegrationService;

    @BeforeEach
    void setUp() {
        ((Map) ReflectionTestUtils.getField(currencyRateService, "currencyRates"))
            .clear();
    }

    @Test
    @DisplayName("Should add currency")
    void shouldAddCurrency() {
        ArgumentCaptor<Currency> currencyArgumentCaptor = ArgumentCaptor.forClass(Currency.class);

        when(currencyDao.save(currencyArgumentCaptor.capture()))
            .then(returnsFirstArg());
        mockCurrenciesCodes();
        when(exchangeRateIntegrationService.getExchangeRateForCurrencies(List.of("EUR", "UAH", "USD")))
            .thenReturn(buildCurrencyRatesMap());

        currencyRateService.addCurrency("USD");

        assertThat(currencyArgumentCaptor.getValue())
            .extracting(Currency::getCurrencyCode)
            .isEqualTo("USD");

        assertThatCurrencyRatesMapEqualsToBuiltMap();
    }

    @Test
    @DisplayName("Should get used currencies")
    void shouldGetUsedCurrencies() {
        mockCurrenciesCodes();

        assertThat(currencyRateService.getUsedCurrencies())
            .hasSize(3)
            .containsExactly("EUR", "UAH", "USD");
    }

    @Test
    @DisplayName("Should get currency rates")
    void shouldGetCurrencyRates() {
        when(currencyDao.checkIfExistsByCurrencyCode("USD"))
            .thenReturn(true);

        ((Map<String, List<CurrencyExchangeRate>>) ReflectionTestUtils.getField(currencyRateService, "currencyRates"))
            .putAll(buildCurrencyRatesMap());

        assertThat(currencyRateService.getCurrencyRates("USD"))
            .extracting(
                CurrencyRateDTO::baseCurrency,
                CurrencyRateDTO::rates)
            .containsExactly(
                "USD",
                Map.of("UAH", RateDTO.builder().rate(new BigDecimal("41.1561")).doubleConversion(true).build(),
                    "EUR", RateDTO.builder().rate(new BigDecimal("0.9523")).doubleConversion(false).build()));
    }

    @Test
    @DisplayName("Get currency rates should refresh currency rates when currencyRates map is empty")
    void getCurrencyRatesShouldRefreshCurrencyRatesWhenCurrencyRatesMapIsEmpty() {
        List<String> currenciesCodes = List.of("EUR", "UAH", "USD");

        when(currencyDao.checkIfExistsByCurrencyCode("USD"))
            .thenReturn(true);
        mockCurrenciesCodes();
        when(exchangeRateIntegrationService.getExchangeRateForCurrencies(currenciesCodes))
            .thenReturn(buildCurrencyRatesMap());

        currencyRateService.getCurrencyRates("USD");

        verify(exchangeRateIntegrationService).getExchangeRateForCurrencies(currenciesCodes);
    }

    @Test
    @DisplayName("Get currency rates should throw exception for not used currencyCode")
    void getCurrencyRatesShouldThrowExceptionForNotUsedCurrencyCode() {
        when(currencyDao.checkIfExistsByCurrencyCode("USD"))
            .thenReturn(false);

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> currencyRateService.getCurrencyRates("USD"))
            .withMessage("""
                Currency 'USD' is not available for getting exchange rates.
                Please try to add currencyCode using existing API.
                """);
    }

    @Test
    @DisplayName("Should have scheduled currency rate fetching method")
    void shouldHaveScheduledCurrencyRateFetchingMethod() {
        ArgumentCaptor<Collection<CurrencyLog>> currencyLogsArgumentCaptor = ArgumentCaptor.forClass(Collection.class);

        mockCurrenciesCodes();
        when(exchangeRateIntegrationService.getExchangeRateForCurrencies(List.of("EUR", "UAH", "USD")))
            .thenReturn(buildCurrencyRatesMap());
        when(currencyLogDao.saveAll(currencyLogsArgumentCaptor.capture()))
            .then(returnsFirstArg());

        ReflectionTestUtils.invokeMethod(currencyRateService, "receiveExchangeRates");

        assertThat(currencyLogsArgumentCaptor.getAllValues())
            .hasSize(3)
            .extracting(Collection::size)
            .containsExactly(2, 2, 2);
        Collection<CurrencyLog> capturedLogs = currencyLogsArgumentCaptor.getAllValues().stream()
                                                                         .reduce(new ArrayList<>(), (identity, collection) -> {
                                                                             identity.addAll(collection);
                                                                             return identity;
                                                                         });
        assertThat(capturedLogs)
            .hasSize(6)
            .extracting(
                CurrencyLog::getBaseCurrencyCode,
                CurrencyLog::getAnotherCurrencyCode,
                CurrencyLog::getRate,
                CurrencyLog::getRateDateTime,
                CurrencyLog::getDoubleConversion)
            .containsExactlyInAnyOrder(
                tuple("EUR", "USD", new BigDecimal("1.05"), Instant.EPOCH, false),
                tuple("EUR", "UAH", new BigDecimal("43.214"), Instant.EPOCH, false),
                tuple("UAH", "USD", new BigDecimal("0.0242"), Instant.EPOCH, true),
                tuple("UAH", "EUR", new BigDecimal("0.0231"), Instant.EPOCH, false),
                tuple("USD", "UAH", new BigDecimal("41.1561"), Instant.EPOCH, true),
                tuple("USD", "EUR", new BigDecimal("0.9523"), Instant.EPOCH, false));

        Method scheduledMethod = ReflectionUtils.getRequiredMethod(CurrencyRateServiceImpl.class, "receiveExchangeRates");
        Scheduled annotation = scheduledMethod.getAnnotation(Scheduled.class);

        assertThat(annotation)
            .isNotNull()
            .extracting(
                Scheduled::fixedDelayString,
                Scheduled::timeUnit)
            .containsExactly(
                "${currencyRate.fetch.frequency}",
                TimeUnit.SECONDS);
    }

    private void assertThatCurrencyRatesMapEqualsToBuiltMap() {
        Map<String, List<CurrencyExchangeRate>> result = (Map<String, List<CurrencyExchangeRate>>) ReflectionTestUtils.getField(currencyRateService, "currencyRates");

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
                tuple("EUR", "USD", Instant.EPOCH, new BigDecimal("1.05"), false),
                tuple("EUR", "UAH", Instant.EPOCH, new BigDecimal("43.214"), false));

        assertThat(result.get("UAH"))
            .hasSize(2)
            .extracting(
                CurrencyExchangeRate::baseCurrency,
                CurrencyExchangeRate::anotherCurrency,
                CurrencyExchangeRate::dateTime,
                CurrencyExchangeRate::rate,
                CurrencyExchangeRate::doubleConversion)
            .containsExactlyInAnyOrder(
                tuple("UAH", "USD", Instant.EPOCH, new BigDecimal("0.0242"), true),
                tuple("UAH", "EUR", Instant.EPOCH, new BigDecimal("0.0231"), false));

        assertThat(result.get("USD"))
            .hasSize(2)
            .extracting(
                CurrencyExchangeRate::baseCurrency,
                CurrencyExchangeRate::anotherCurrency,
                CurrencyExchangeRate::dateTime,
                CurrencyExchangeRate::rate,
                CurrencyExchangeRate::doubleConversion)
            .containsExactlyInAnyOrder(
                tuple("USD", "UAH", Instant.EPOCH, new BigDecimal("41.1561"), true),
                tuple("USD", "EUR", Instant.EPOCH, new BigDecimal("0.9523"), false));
    }

    private void mockCurrenciesCodes() {
        when(currencyDao.getCurrencies())
            .thenReturn(List.of(
                new Currency("EUR"),
                new Currency("UAH"),
                new Currency("USD")));
    }

    private static Map<String, List<CurrencyExchangeRate>> buildCurrencyRatesMap() {
        return Map.of(
            "EUR", List.of(
                new CurrencyExchangeRate("EUR", "USD", Instant.EPOCH, new BigDecimal("1.05"), false),
                new CurrencyExchangeRate("EUR", "UAH", Instant.EPOCH, new BigDecimal("43.214"), false)),
            "UAH", List.of(
                new CurrencyExchangeRate("UAH", "USD", Instant.EPOCH, new BigDecimal("0.0242"), true),
                new CurrencyExchangeRate("UAH", "EUR", Instant.EPOCH, new BigDecimal("0.0231"), false)),
            "USD", List.of(
                new CurrencyExchangeRate("USD", "UAH", Instant.EPOCH, new BigDecimal("41.1561"), true),
                new CurrencyExchangeRate("USD", "EUR", Instant.EPOCH, new BigDecimal("0.9523"), false)));
    }


}
