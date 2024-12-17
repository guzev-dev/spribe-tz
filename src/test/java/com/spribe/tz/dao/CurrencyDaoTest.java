package com.spribe.tz.dao;

import com.spribe.tz.dao.entity.Currency;
import com.spribe.tz.dao.repo.CurrencyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db/initialize.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CurrencyDaoTest {

    @Autowired
    private CurrencyDao currencyDao;

    @SpyBean
    private CurrencyRepository currencyRepository;

    @Test
    @DisplayName("Should save currency if not exists")
    void shouldSaveCurrencyIfNotExists() {
        Currency newCurrency = new Currency("JPY");

        assertThat(currencyDao.save(newCurrency))
            .extracting(Currency::getCurrencyCode)
            .isEqualTo("JPY");

        verify(currencyRepository).save(newCurrency);
    }

    @Test
    @DisplayName("Should not save currency if already exists")
    void shouldNotSaveCurrencyIfAlreadyExists() {
        Currency newCurrency = new Currency("USD");

        assertThat(currencyDao.save(newCurrency))
            .extracting(Currency::getCurrencyCode)
            .isEqualTo("USD");

        verify(currencyRepository, times(0)).save(newCurrency);
    }

    @Test
    @DisplayName("Should get currencies")
    void shouldGetCurrencies() {
        assertThat(currencyDao.getCurrencies())
            .hasSize(3)
            .extracting(Currency::getCurrencyCode)
            .containsExactlyInAnyOrder("EUR", "UAH", "USD");
    }

    @Test
    @DisplayName("Check if exists by currency code should return true for existing currency code")
    void checkIfExistsByCurrencyCodeShouldReturnTrueForExistingCurrencyCode() {
        Assertions.assertTrue(currencyDao.checkIfExistsByCurrencyCode("USD"));
    }

    @Test
    @DisplayName("Check if exists by currency code should return false for non-existing currency code")
    void checkIfExistsByCurrencyCodeShouldReturnFalseForNonExistingCurrencyCode() {
        Assertions.assertFalse(currencyDao.checkIfExistsByCurrencyCode("JPY"));
    }

}
