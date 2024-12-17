package com.spribe.tz.controller;

import com.spribe.tz.controller.dto.CurrencyRateDTO;
import com.spribe.tz.controller.dto.RateDTO;
import com.spribe.tz.service.CurrencyRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest
@ActiveProfiles("test")
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyRateService currencyRateService;

    @Test
    @DisplayName("Should add currency")
    void shouldAddCurrency() throws Exception {
        mockMvc.perform(post("/currencies")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{\"currencyCode\":\"USD\"}"))
               .andExpect(status().isAccepted());

        verify(currencyRateService).addCurrency("USD");
    }

    @Test
    @DisplayName("Should get used currencies")
    void shouldGetUsedCurrencies() throws Exception {
        when(currencyRateService.getUsedCurrencies())
            .thenReturn(List.of("EUR", "UAH", "USD"));

        mockMvc.perform(get("/currencies"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$", hasSize(3)))
               .andExpect(jsonPath("$[0]").value("EUR"))
               .andExpect(jsonPath("$[1]").value("UAH"))
               .andExpect(jsonPath("$[2]").value("USD"));

        verify(currencyRateService).getUsedCurrencies();
    }

    @Test
    @DisplayName("Should get currency exchange rates for currency")
    void shouldGetCurrencyExchangeRatesForCurrency() throws Exception {
        when(currencyRateService.getCurrencyRates("USD"))
            .thenReturn(CurrencyRateDTO.builder()
                                       .baseCurrency("USD")
                                       .rates(Map.of(
                                           "EUR", RateDTO.builder().rate(new BigDecimal("0.983")).doubleConversion(false).build(),
                                           "UAH", RateDTO.builder().rate(new BigDecimal("41.532")).doubleConversion(true).build()))
                                       .build());

        mockMvc.perform(get("/currencies/USD/rates"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.baseCurrency").value("USD"))
               .andExpect(jsonPath("$.rates").isMap())
               .andExpect(jsonPath("$.rates.EUR.rate").value("0.983"))
               .andExpect(jsonPath("$.rates.EUR.doubleConversion").value("false"))
               .andExpect(jsonPath("$.rates.UAH.rate").value("41.532"))
               .andExpect(jsonPath("$.rates.UAH.doubleConversion").value("true"));

        verify(currencyRateService).getCurrencyRates("USD");
    }
}
