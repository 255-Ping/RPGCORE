package com.github._255_ping.rpg.api.formula;

import java.util.Map;

public interface ExpressionEvaluator {
    double evaluate(String expression, Map<String, Double> variables);
    Compiled compile(String expression);

    interface Compiled {
        double evaluate(Map<String, Double> variables);
    }
}
