package com.spribe.tz.dao;

import com.spribe.tz.dao.entity.Currency;
import com.spribe.tz.dao.repo.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyDao {

    private final CurrencyRepository currencyRepository;

    public Currency save(final Currency currency) {
        return currencyRepository.existsById(currency.getCurrencyCode())
            ? currency
            : currencyRepository.save(currency);
    }

    public List<Currency> getCurrencies() {
        return currencyRepository.findAll();
    }

    public boolean checkIfExistsByCurrencyCode(final String currencyCode) {
        return currencyRepository.existsById(currencyCode);
    }

}
