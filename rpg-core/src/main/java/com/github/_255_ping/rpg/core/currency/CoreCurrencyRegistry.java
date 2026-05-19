package com.github._255_ping.rpg.core.currency;

import com.github._255_ping.rpg.api.currency.Currency;
import com.github._255_ping.rpg.api.currency.CurrencyRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class CoreCurrencyRegistry implements CurrencyRegistry {

    private final ConcurrentMap<String, Currency> byId = new ConcurrentHashMap<>();
    private final AtomicReference<Currency> primary = new AtomicReference<>();

    @Override
    public void register(Currency currency) {
        byId.put(currency.id(), currency);
        // First registered currency is the primary; later registrations don't override.
        primary.compareAndSet(null, currency);
    }

    @Override
    public Optional<Currency> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Currency> primary() {
        return Optional.ofNullable(primary.get());
    }

    @Override
    public Collection<Currency> all() {
        return byId.values();
    }
}
