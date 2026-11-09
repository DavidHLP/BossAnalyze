package com.david.hlp.commons.recruitment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.ArrayList;
import java.util.List;

public final class JobArrays {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private JobArrays() {}

    public static boolean isJsonArray(String value) {
        if (value == null) return false;
        try {
            JsonNode node = JSON.readTree(value);
            if (node == null || !node.isArray()) return false;
            for (JsonNode item : node) if (!item.isTextual()) return false;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static List<String> read(String value) {
        if (value == null || value.isBlank()) return List.of();
        // List.toString is ambiguous: retain the raw legacy value instead of guessing boundaries.
        if (!isJsonArray(value)) return List.of(value);
        try {
            List<String> values = new ArrayList<>();
            for (JsonNode item : JSON.readTree(value)) values.add(item.textValue());
            return values;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid job array", e);
        }
    }
}
