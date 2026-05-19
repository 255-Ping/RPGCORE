package com.github._255_ping.rpg.api.abilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AbilityDsl {

    private AbilityDsl() {}

    public static List<AbilityInvocation> parse(String input) {
        List<AbilityInvocation> out = new ArrayList<>();
        int i = 0;
        int n = input.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(input.charAt(i))) i++;
            if (i >= n) break;
            int nameStart = i;
            while (i < n && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) i++;
            String name = input.substring(nameStart, i);
            if (name.isEmpty()) {
                throw new IllegalArgumentException("expected effect name at index " + i + " in: " + input);
            }
            Map<String, String> params = new LinkedHashMap<>();
            if (i < n && input.charAt(i) == '{') {
                i++;
                while (i < n && input.charAt(i) != '}') {
                    while (i < n && Character.isWhitespace(input.charAt(i))) i++;
                    int kStart = i;
                    while (i < n && input.charAt(i) != '=' && input.charAt(i) != ',' && input.charAt(i) != '}') i++;
                    String k = input.substring(kStart, i).trim();
                    String v = "";
                    if (i < n && input.charAt(i) == '=') {
                        i++;
                        int vStart = i;
                        while (i < n && input.charAt(i) != ',' && input.charAt(i) != '}') i++;
                        v = input.substring(vStart, i).trim();
                    }
                    if (!k.isEmpty()) params.put(k, v);
                    if (i < n && input.charAt(i) == ',') i++;
                }
                if (i >= n) throw new IllegalArgumentException("unterminated '{' for effect " + name);
                i++;
            }
            out.add(new AbilityInvocation(name, params));
        }
        return out;
    }

    public static double doubleParam(Map<String, String> params, String key, double fallback) {
        String v = params.get(key);
        if (v == null || v.isEmpty()) return fallback;
        return Double.parseDouble(v);
    }

    public static int intParam(Map<String, String> params, String key, int fallback) {
        String v = params.get(key);
        if (v == null || v.isEmpty()) return fallback;
        return Integer.parseInt(v);
    }

    public static boolean boolParam(Map<String, String> params, String key, boolean fallback) {
        String v = params.get(key);
        if (v == null || v.isEmpty()) return fallback;
        return Boolean.parseBoolean(v);
    }
}
