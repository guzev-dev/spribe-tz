package com.spribe.tz.dao;

import com.spribe.tz.dao.entity.CurrencyLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db/initialize.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CurrencyLogDaoTest {

    @Autowired
    private CurrencyLogDao currencyLogDao;

    @Test
    @DisplayName("Should save currency logs")
    void shouldSaveCurrencyLogs() {
        Instant now = Instant.now();
        List<CurrencyLog> logs = List.of(
            new CurrencyLog(null, "USD", "EUR", new BigDecimal("0.983"), now, false, null),
            new CurrencyLog(null, "USD", "UAH", new BigDecimal("41.532"), now, true, null));

        Collection<CurrencyLog> result = currencyLogDao.saveAll(logs);

        assertThat(result)
            .hasSize(2)
            .extracting(
                CurrencyLog::getBaseCurrencyCode,
                CurrencyLog::getAnotherCurrencyCode,
                CurrencyLog::getRate,
                CurrencyLog::getRateDateTime,
                CurrencyLog::getDoubleConversion)
            .containsExactlyInAnyOrder(
                tuple("USD", "EUR", new BigDecimal("0.983"), now, false),
                tuple("USD", "UAH", new BigDecimal("41.532"), now, true));

        assertThat(result)
            .extracting(
                CurrencyLog::getId,
                CurrencyLog::getCreationDate)
            .isNotNull();
    }

}
