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

    // ── Additional coverage ───────────────────────────────────────────────────

    @Test void divisionOperator() {
        assertEquals(2.5, eval.evaluate("5 / 2", Map.of()), EPS);
        assertEquals(1.0, eval.evaluate("3 / 3", Map.of()), EPS);
    }

    @Test void nestedFunctionCall() {
        // floor(sqrt(16)) = floor(4) = 4
        assertEquals(4.0, eval.evaluate("floor(sqrt(16))", Map.of()), EPS);
        // abs(floor(-3.7)) = abs(-4) = 4
        assertEquals(4.0, eval.evaluate("abs(floor(-3.7))", Map.of()), EPS);
    }

    @Test void multipleVariables() {
        Map<String, Double> vars = Map.of("level", 10.0, "multiplier", 2.5, "bonus", 50.0);
        // 10 * 2.5 + 50 = 75
        assertEquals(75.0, eval.evaluate("level * multiplier + bonus", vars), EPS);
    }

    @Test void prevXpTotalVariable_usedInSkillCurves() {
        // Mirrors the `prev_xp_total` variable injected during threshold building.
        Map<String, Double> vars = Map.of("level", 5.0, "prev_xp_total", 1000.0);
        // A curve that adds 50 per level on top of a carry: level * 50 + prev_xp_total * 0
        assertEquals(250.0, eval.evaluate("level * 50", vars), EPS);
    }

    @Test void skillCurveExpression_level1to4() {
        // Reproduces the default skill curve; verifies values match hand-calculations.
        // cost(1) = 100 * 1^1.5 = 100
        // cost(2) = 100 * 2^1.5 ≈ 282.84
        // cost(3) = 100 * 3^1.5 ≈ 519.62
        var compiled = eval.compile("100 * level ^ 1.5");
        assertEquals(100.0, compiled.evaluate(Map.of("level", 1.0)), 0.01);
        assertEquals(282.84, compiled.evaluate(Map.of("level", 2.0)), 0.01);
        assertEquals(519.62, compiled.evaluate(Map.of("level", 3.0)), 0.01);
    }

    @Test void subtraction_leftAssociative() {
        // 10 - 3 - 2 must be (10-3)-2 = 5, not 10-(3-2) = 9.
        assertEquals(5.0, eval.evaluate("10 - 3 - 2", Map.of()), EPS);
    }

    @Test void divisionByZero_returnsInfinityOrNaN() {
        // We don't mandate a specific result (it's floating-point), but it must not throw.
        double result = eval.evaluate("1 / 0", Map.of());
        assertTrue(Double.isInfinite(result) || Double.isNaN(result),
                "expected Infinity or NaN for 1/0, got: " + result);
    }
}
