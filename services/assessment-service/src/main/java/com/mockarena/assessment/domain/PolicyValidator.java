package com.mockarena.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Locale;

public final class PolicyValidator {
    private PolicyValidator() { }
    public static String timing(String code, JsonNode parameters, Integer attemptDurationSeconds) {
        String normalized = code(code, "Timing policy"); object(parameters);
        return switch (normalized) {
            case "UNTIMED" -> { empty(parameters, "UNTIMED timing policy"); if (attemptDurationSeconds != null) throw new IllegalArgumentException("UNTIMED timing policy must not include an attempt duration"); yield normalized; }
            case "FIXED_DURATION" -> { empty(parameters, "FIXED_DURATION timing policy"); positiveDuration(attemptDurationSeconds); yield normalized; }
            default -> throw new IllegalArgumentException("Unsupported timing policy");
        };
    }
    public static String attempt(String code, JsonNode parameters) {
        String normalized = code(code, "Attempt policy"); object(parameters);
        if (!"MAX_ATTEMPTS".equals(normalized)) throw new IllegalArgumentException("Unsupported attempt policy");
        positiveInteger(parameters, "maxAttempts", 1, 100, "MAX_ATTEMPTS attempt policy"); return normalized;
    }
    public static String release(String code, JsonNode parameters) {
        String normalized = code(code, "Result release policy"); object(parameters);
        return switch (normalized) {
            case "IMMEDIATE", "MANUAL" -> { empty(parameters, normalized + " result release policy"); yield normalized; }
            case "SCHEDULED" -> { JsonNode releaseAt = parameters.get("releaseAt"); if (releaseAt == null || !releaseAt.isTextual()) throw new IllegalArgumentException("SCHEDULED result release policy requires releaseAt"); try { Instant.parse(releaseAt.asText()); } catch (RuntimeException exception) { throw new IllegalArgumentException("SCHEDULED result release policy requires an ISO-8601 releaseAt"); } yield normalized; }
            default -> throw new IllegalArgumentException("Unsupported result release policy");
        };
    }
    public static String assessmentType(String value) { return code(value, "Assessment type"); }
    public static void availability(Instant availableFrom, Instant availableUntil) { if (availableFrom != null && availableUntil != null && !availableFrom.isBefore(availableUntil)) throw new IllegalArgumentException("availableFrom must be before availableUntil"); }
    private static String code(String value, String label) { if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required"); return value.trim().toUpperCase(Locale.ROOT); }
    private static void object(JsonNode value) { if (value == null || !value.isObject()) throw new IllegalArgumentException("Policy parameters must be an object"); }
    private static void empty(JsonNode value, String label) { if (!value.isEmpty()) throw new IllegalArgumentException(label + " does not accept parameters"); }
    private static void positiveInteger(JsonNode value, String field, int min, int max, String label) { JsonNode number = value.get(field); if (value.size() != 1 || number == null || !number.canConvertToInt() || number.asInt() < min || number.asInt() > max) throw new IllegalArgumentException(label + " requires " + field); }
    private static void positiveDuration(Integer value) { if (value == null || value < 1 || value > 86_400) throw new IllegalArgumentException("FIXED_DURATION timing policy requires attemptDurationSeconds"); }
}
