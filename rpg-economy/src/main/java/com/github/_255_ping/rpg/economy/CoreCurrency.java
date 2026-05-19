package com.github._255_ping.rpg.economy;

import com.github._255_ping.rpg.api.currency.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CoreCurrency implements Currency {

    private final String id;
    private final String displaySingular;
    private final String displayPlural;
    private final String prefix;
    private final String suffix;
    private final int decimals;
    private final BigDecimal maxBalance;

    public CoreCurrency(String id, String displaySingular, String displayPlural,
                         String prefix, String suffix, int decimals, BigDecimal maxBalance) {
        this.id = id;
        this.displaySingular = displaySingular;
        this.displayPlural = displayPlural;
        this.prefix = prefix;
        this.suffix = suffix;
        this.decimals = decimals;
        this.maxBalance = maxBalance;
    }

    @Override public String id() { return id; }
    @Override public String displaySingular() { return displaySingular; }
    @Override public String displayPlural() { return displayPlural; }
    @Override public String prefix() { return prefix; }
    @Override public String suffix() { return suffix; }
    @Override public int decimals() { return decimals; }
    @Override public BigDecimal maxBalance() { return maxBalance; }

    @Override
    public String format(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(decimals, RoundingMode.DOWN);
        return prefix + scaled.toPlainString() + suffix;
    }
}
