package com.spribe.tz.dao;

import com.spribe.tz.dao.entity.CurrencyLog;
import com.spribe.tz.dao.repo.CurrencyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class CurrencyLogDao {

    private final CurrencyLogRepository currencyLogRepository;

    public Collection<CurrencyLog> saveAll(final Collection<CurrencyLog> currencyLogs) {
        return currencyLogRepository.saveAll(currencyLogs);
    }

}
