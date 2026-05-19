package com.github._255_ping.rpg.api.currency;

import java.util.Collection;
import java.util.Optional;

public interface CurrencyRegistry {
    void register(Currency currency);
    Optional<Currency> get(String id);
    Optional<Currency> primary();
    Collection<Currency> all();
}
