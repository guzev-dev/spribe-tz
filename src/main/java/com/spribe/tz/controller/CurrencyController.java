package com.spribe.tz.controller;

import com.spribe.tz.controller.dto.AddCurrencyRequestDTO;
import com.spribe.tz.controller.dto.CurrencyRateDTO;
import com.spribe.tz.service.CurrencyRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyRateService currencyRateService;

    @Operation(summary = "Add new currency for getting exchange rates")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addCurrency(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Currency to be added for getting exchange rates. Check available currencies codes on fixer.io",
                                                              required = true, content = @Content(examples = @ExampleObject(value = "{ \"currencyCode\": \"EUR\" }")))
        @RequestBody final AddCurrencyRequestDTO request) {
        currencyRateService.addCurrency(request.currencyCode());
    }

    @Operation(summary = "Get a list of currencies used in the project")
    @GetMapping
    public List<String> getUsedCurrencies() {
        return currencyRateService.getUsedCurrencies();
    }

    @Operation(summary = "Get exchange rates for a currency")
    @GetMapping("/{currencyCode}/rates")
    public CurrencyRateDTO getCurrencyRates(
        @Parameter(description = "Base currency code (e.g. \"USD\")")
        @PathVariable("currencyCode") final String currencyCode) {
        return currencyRateService.getCurrencyRates(currencyCode);
    }

}
