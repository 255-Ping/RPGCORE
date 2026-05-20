package com.github._255_ping.rpg.core.formula;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the small expression DSL used for level curves and other configurable formulas. This is
 * a sandboxed evaluator — no reflection, no scripting — so the test suite primarily checks
 * arithmetic correctness, variable substitution, and that unknown identifiers fail loudly.
 */
class CoreExpressionEvaluatorTest {

    private final CoreExpressionEvaluator eval = new CoreExpressionEvaluator();

    private static final double EPS = 1e-9;

    @Test void literalNumber() {
        assertEquals(42.0, eval.evaluate("42", Map.of()), EPS);
    }

    @Test void basicArithmeticPrecedence() {
        assertEquals(14.0, eval.evaluate("2 + 3 * 4", Map.of()), EPS);
        assertEquals(20.0, eval.evaluate("(2 + 3) * 4", Map.of()), EPS);
    }

    @Test void powerOperatorIsRightAssociative() {
        // 2 ^ 3 ^ 2 == 2 ^ (3 ^ 2) == 2 ^ 9 == 512
        assertEquals(512.0, eval.evaluate("2 ^ 3 ^ 2", Map.of()), EPS);
    }

    @Test void unaryMinus() {
        assertEquals(-5.0, eval.evaluate("-5", Map.of()), EPS);
        assertEquals(-1.0, eval.evaluate("2 + -3", Map.of()), EPS);
    }

    @Test void variableSubstitution() {
        // level=1 → 1^1.5 = 1 → 100*1 = 100
        assertEquals(100.0, eval.evaluate("100 * level ^ 1.5",
                Map.of("level", 1.0)), 0.001);
        // level=4 → 4^1.5 = 8 → 100*8 = 800
        assertEquals(800.0, eval.evaluate("100 * level ^ 1.5",
                Map.of("level", 4.0)), 0.001);
    }

    @Test void builtinFunctions() {
        // min/max are strictly 2-ary in this evaluator (intentional — see CoreExpressionEvaluator).
        assertEquals(2.0, eval.evaluate("sqrt(4)", Map.of()), EPS);
        assertEquals(8.0, eval.evaluate("pow(2, 3)", Map.of()), EPS);
        assertEquals(3.0, eval.evaluate("max(2, 3)", Map.of()), EPS);
        assertEquals(1.0, eval.evaluate("min(1, 2)", Map.of()), EPS);
        assertEquals(5.0, eval.evaluate("abs(-5)", Map.of()), EPS);
        assertEquals(3.0, eval.evaluate("floor(3.9)", Map.of()), EPS);
        assertEquals(4.0, eval.evaluate("ceil(3.1)", Map.of()), EPS);
    }

    @Test void unknownVariableThrows() {
        assertThrows(RuntimeException.class,
                () -> eval.evaluate("missing + 1", Map.of()));
    }

    @Test void compiledCacheable() {
        var compiled = eval.compile("100 * level ^ 1.5");
        // Running twice with different vars must produce different results.
        double a = compiled.evaluate(Map.of("level", 1.0));
        double b = compiled.evaluate(Map.of("level", 4.0));
        assertTrue(b > a);
    }
}
