package com.spribe.tz.dao.repo;

import com.spribe.tz.dao.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {}
