package framework;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonSerializer {

    private JsonSerializer() {
    }

    public static String toJson(Object value) {
        StringBuilder output = new StringBuilder();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        writeValue(value, output, seen);
        return output.toString();
    }

    private static void writeValue(Object value, StringBuilder output, Set<Object> seen) {
        if (value == null) {
            output.append("null");
            return;
        }

        if (value instanceof String string) {
            writeString(string, output);
            return;
        }

        if (value instanceof Character character) {
            writeString(String.valueOf(character), output);
            return;
        }

        if (value instanceof Number number) {
            writeNumber(number, output);
            return;
        }

        if (value instanceof Boolean booleanValue) {
            output.append(booleanValue);
            return;
        }

        if (value instanceof Enum<?> enumValue) {
            writeString(enumValue.name(), output);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            writeMap(map, output, seen);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            writeIterable(iterable, output, seen);
            return;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            writeArray(value, output, seen);
            return;
        }

        writeObject(value, output, seen);
    }

    private static void writeMap(Map<?, ?> map, StringBuilder output, Set<Object> seen) {
        if (!seen.add(map)) {
            output.append("null");
            return;
        }

        output.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                output.append(',');
            }
            first = false;
            writeString(String.valueOf(entry.getKey()), output);
            output.append(':');
            writeValue(entry.getValue(), output, seen);
        }
        output.append('}');
        seen.remove(map);
    }

    private static void writeIterable(Iterable<?> iterable, StringBuilder output, Set<Object> seen) {
        if (!seen.add(iterable)) {
            output.append("null");
            return;
        }

        output.append('[');
        boolean first = true;
        for (Object item : iterable) {
            if (!first) {
                output.append(',');
            }
            first = false;
            writeValue(item, output, seen);
        }
        output.append(']');
        seen.remove(iterable);
    }

    private static void writeArray(Object array, StringBuilder output, Set<Object> seen) {
        if (!seen.add(array)) {
            output.append("null");
            return;
        }

        output.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                output.append(',');
            }
            writeValue(Array.get(array, i), output, seen);
        }
        output.append(']');
        seen.remove(array);
    }

    private static void writeObject(Object value, StringBuilder output, Set<Object> seen) {
        if (!seen.add(value)) {
            output.append("null");
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        for (Method method : value.getClass().getMethods()) {
            if (shouldIgnore(method)) {
                continue;
            }

            String propertyName = propertyName(method);
            if (propertyName == null || properties.containsKey(propertyName)) {
                continue;
            }

            try {
                properties.put(propertyName, method.invoke(value));
            } catch (IllegalAccessException | InvocationTargetException e) {
                properties.put(propertyName, null);
            }
        }

        writeMap(properties, output, seen);
        seen.remove(value);
    }

    private static boolean shouldIgnore(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
            return true;
        }

        if (method.getDeclaringClass() == Object.class) {
            return true;
        }

        String name = method.getName();
        return !((name.startsWith("get") && name.length() > 3 && method.getReturnType() != Void.TYPE)
                || (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)));
    }

    private static String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    private static void writeNumber(Number number, StringBuilder output) {
        if (number instanceof Double doubleValue) {
            if (doubleValue.isNaN() || doubleValue.isInfinite()) {
                output.append("null");
                return;
            }
        } else if (number instanceof Float floatValue) {
            if (floatValue.isNaN() || floatValue.isInfinite()) {
                output.append("null");
                return;
            }
        }

        if (number instanceof BigDecimal || number instanceof BigInteger
                || number instanceof Byte || number instanceof Short
                || number instanceof Integer || number instanceof Long
                || number instanceof Float || number instanceof Double) {
            output.append(number.toString());
            return;
        }

        output.append(number.toString());
    }

    private static void writeString(String value, StringBuilder output) {
        output.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }
}