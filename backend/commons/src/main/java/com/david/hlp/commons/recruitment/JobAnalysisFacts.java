package com.david.hlp.commons.recruitment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

/** The same factual roots consumed by Python candidates; snapshots remain complete. */
public final class JobAnalysisFacts {
    private JobAnalysisFacts() {}

    public static JsonNode select(JsonNode detail) {
        var facts = JsonNodeFactory.instance.objectNode();
        for (String key : List.of("basicInfo", "jobDescription", "jobTags")) {
            facts.set(key, canonical(detail.path(key).isMissingNode()
                    ? JsonNodeFactory.instance.nullNode() : detail.path(key)));
        }
        return facts;
    }

    private static JsonNode canonical(JsonNode value) {
        if (value.isObject()) {
            var object = JsonNodeFactory.instance.objectNode();
            var keys = new TreeSet<String>();
            value.fieldNames().forEachRemaining(keys::add);
            keys.forEach(key -> object.set(key, canonical(value.get(key))));
            return object;
        }
        if (value.isArray()) {
            var array = JsonNodeFactory.instance.arrayNode();
            value.forEach(child -> array.add(canonical(child)));
            return array;
        }
        return value;
    }

    public static boolean same(JsonNode left, JsonNode right) {
        return left != null && right != null && left.isObject() && right.isObject()
                && select(left).equals(select(right));
    }

    public static String digest(JsonNode detail) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(select(detail).toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
