package com.github._255_ping.rpg.core.formula;

import com.github._255_ping.rpg.api.formula.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recursive-descent expression evaluator. Whitelisted operators ({@code + - * / % ^})
 * and functions ({@code pow sqrt min max floor ceil round abs log ln}). Variables are
 * passed at evaluation time. No I/O, no scripting, no reflection — only the listed
 * symbols are recognized.
 */
public final class CoreExpressionEvaluator implements ExpressionEvaluator {

    @Override
    public double evaluate(String expression, Map<String, Double> variables) {
        return compile(expression).evaluate(variables);
    }

    @Override
    public Compiled compile(String expression) {
        List<Token> tokens = tokenize(expression);
        Parser p = new Parser(tokens, expression);
        Node ast = p.parseFull();
        return vars -> ast.eval(vars == null ? Map.of() : vars);
    }

    // ----- Tokens -----

    private enum TokenKind { NUMBER, IDENT, OP, LPAREN, RPAREN, COMMA }

    private record Token(TokenKind kind, String text, int pos) {}

    private static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                out.add(new Token(TokenKind.NUMBER, s.substring(start, i), start));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
                out.add(new Token(TokenKind.IDENT, s.substring(start, i), start));
                continue;
            }
            switch (c) {
                case '+', '-', '*', '/', '%', '^' -> out.add(new Token(TokenKind.OP, String.valueOf(c), i++));
                case '(' -> out.add(new Token(TokenKind.LPAREN, "(", i++));
                case ')' -> out.add(new Token(TokenKind.RPAREN, ")", i++));
                case ',' -> out.add(new Token(TokenKind.COMMA, ",", i++));
                default -> throw new IllegalArgumentException("Unexpected char '" + c + "' at " + i + " in: " + s);
            }
        }
        return out;
    }

    // ----- AST -----

    private interface Node {
        double eval(Map<String, Double> vars);
    }

    private record NumNode(double value) implements Node {
        @Override public double eval(Map<String, Double> vars) { return value; }
    }

    private record VarNode(String name) implements Node {
        @Override public double eval(Map<String, Double> vars) {
            Double v = vars.get(name);
            if (v == null) throw new IllegalArgumentException("Unbound variable: " + name);
            return v;
        }
    }

    private record BinaryNode(char op, Node left, Node right) implements Node {
        @Override public double eval(Map<String, Double> vars) {
            double a = left.eval(vars);
            double b = right.eval(vars);
            return switch (op) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> a / b;
                case '%' -> a % b;
                case '^' -> Math.pow(a, b);
                default -> throw new IllegalStateException("op " + op);
            };
        }
    }

    private record UnaryNegNode(Node inner) implements Node {
        @Override public double eval(Map<String, Double> vars) { return -inner.eval(vars); }
    }

    private record CallNode(String name, List<Node> args) implements Node {
        @Override public double eval(Map<String, Double> vars) {
            double[] a = new double[args.size()];
            for (int i = 0; i < args.size(); i++) a[i] = args.get(i).eval(vars);
            return switch (name) {
                case "pow"   -> req(a, 2, name) ? Math.pow(a[0], a[1]) : 0;
                case "sqrt"  -> req(a, 1, name) ? Math.sqrt(a[0]) : 0;
                case "min"   -> req(a, 2, name) ? Math.min(a[0], a[1]) : 0;
                case "max"   -> req(a, 2, name) ? Math.max(a[0], a[1]) : 0;
                case "floor" -> req(a, 1, name) ? Math.floor(a[0]) : 0;
                case "ceil"  -> req(a, 1, name) ? Math.ceil(a[0]) : 0;
                case "round" -> req(a, 1, name) ? Math.round(a[0]) : 0;
                case "abs"   -> req(a, 1, name) ? Math.abs(a[0]) : 0;
                case "log"   -> req(a, 1, name) ? Math.log10(a[0]) : 0;
                case "ln"    -> req(a, 1, name) ? Math.log(a[0]) : 0;
                default -> throw new IllegalArgumentException("Unknown function: " + name);
            };
        }

        private static boolean req(double[] a, int n, String name) {
            if (a.length != n) {
                throw new IllegalArgumentException(name + "() expects " + n + " args, got " + a.length);
            }
            return true;
        }
    }

    // ----- Parser -----

    private static final class Parser {
        private final List<Token> tokens;
        private final String src;
        private int pos = 0;

        Parser(List<Token> tokens, String src) {
            this.tokens = tokens;
            this.src = src;
        }

        Node parseFull() {
            Node n = parseExpr();
            if (pos < tokens.size()) {
                throw new IllegalArgumentException("Trailing tokens at position " + tokens.get(pos).pos + " in: " + src);
            }
            return n;
        }

        private Node parseExpr() {
            Node left = parseTerm();
            while (peekOp('+') || peekOp('-')) {
                char op = tokens.get(pos++).text.charAt(0);
                Node right = parseTerm();
                left = new BinaryNode(op, left, right);
            }
            return left;
        }

        private Node parseTerm() {
            Node left = parsePower();
            while (peekOp('*') || peekOp('/') || peekOp('%')) {
                char op = tokens.get(pos++).text.charAt(0);
                Node right = parsePower();
                left = new BinaryNode(op, left, right);
            }
            return left;
        }

        private Node parsePower() {
            Node left = parseUnary();
            if (peekOp('^')) {
                pos++;
                Node right = parsePower();   // right-associative
                return new BinaryNode('^', left, right);
            }
            return left;
        }

        private Node parseUnary() {
            if (peekOp('-')) { pos++; return new UnaryNegNode(parseUnary()); }
            if (peekOp('+')) { pos++; return parseUnary(); }
            return parseAtom();
        }

        private Node parseAtom() {
            if (pos >= tokens.size()) {
                throw new IllegalArgumentException("Unexpected end of expression: " + src);
            }
            Token t = tokens.get(pos++);
            switch (t.kind) {
                case NUMBER -> {
                    return new NumNode(Double.parseDouble(t.text));
                }
                case IDENT -> {
                    if (pos < tokens.size() && tokens.get(pos).kind == TokenKind.LPAREN) {
                        pos++; // consume (
                        List<Node> args = new ArrayList<>();
                        if (pos < tokens.size() && tokens.get(pos).kind != TokenKind.RPAREN) {
                            args.add(parseExpr());
                            while (pos < tokens.size() && tokens.get(pos).kind == TokenKind.COMMA) {
                                pos++;
                                args.add(parseExpr());
                            }
                        }
                        expect(TokenKind.RPAREN, ")");
                        return new CallNode(t.text, args);
                    }
                    return new VarNode(t.text);
                }
                case LPAREN -> {
                    Node inner = parseExpr();
                    expect(TokenKind.RPAREN, ")");
                    return inner;
                }
                default -> throw new IllegalArgumentException("Unexpected token '" + t.text + "' at position " + t.pos + " in: " + src);
            }
        }

        private boolean peekOp(char c) {
            return pos < tokens.size()
                    && tokens.get(pos).kind == TokenKind.OP
                    && tokens.get(pos).text.charAt(0) == c;
        }

        private void expect(TokenKind kind, String what) {
            if (pos >= tokens.size() || tokens.get(pos).kind != kind) {
                int p = pos < tokens.size() ? tokens.get(pos).pos : src.length();
                throw new IllegalArgumentException("Expected '" + what + "' at position " + p + " in: " + src);
            }
            pos++;
        }
    }
}
