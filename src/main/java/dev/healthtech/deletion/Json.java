package dev.healthtech.deletion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private final String text;
    private int cursor;

    private Json(String text) { this.text = text; }

    static Object parse(String text) {
        Json parser = new Json(text);
        Object value = parser.value();
        parser.space();
        if (parser.cursor != text.length()) throw new IllegalArgumentException("Trailing JSON data");
        return value;
    }

    private Object value() {
        space();
        if (cursor >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        return switch (text.charAt(cursor)) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> values = new LinkedHashMap<>();
        cursor++;
        space();
        if (take('}')) return values;
        do {
            space();
            String key = string();
            space();
            expect(':');
            values.put(key, value());
            space();
        } while (take(','));
        expect('}');
        return values;
    }

    private List<Object> array() {
        List<Object> values = new ArrayList<>();
        cursor++;
        space();
        if (take(']')) return values;
        do {
            values.add(value());
            space();
        } while (take(','));
        expect(']');
        return values;
    }

    private String string() {
        expect('"');
        StringBuilder value = new StringBuilder();
        while (cursor < text.length()) {
            char ch = text.charAt(cursor++);
            if (ch == '"') return value.toString();
            if (ch != '\\') { value.append(ch); continue; }
            char escaped = text.charAt(cursor++);
            switch (escaped) {
                case '"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> value.append((char) Integer.parseInt(text.substring(cursor, cursor += 4), 16));
                default -> throw new IllegalArgumentException("Invalid JSON escape");
            }
        }
        throw new IllegalArgumentException("Unclosed JSON string");
    }

    private Object number() {
        int start = cursor;
        while (cursor < text.length() && "-+0123456789.eE".indexOf(text.charAt(cursor)) >= 0) cursor++;
        String token = text.substring(start, cursor);
        return token.contains(".") || token.contains("e") || token.contains("E")
                ? Double.parseDouble(token) : Long.parseLong(token);
    }

    private Object literal(String token, Object value) {
        if (!text.startsWith(token, cursor)) throw new IllegalArgumentException("Invalid JSON literal");
        cursor += token.length();
        return value;
    }

    private void space() { while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++; }
    private boolean take(char expected) { if (cursor < text.length() && text.charAt(cursor) == expected) { cursor++; return true; } return false; }
    private void expect(char expected) { if (!take(expected)) throw new IllegalArgumentException("Expected " + expected); }
}
