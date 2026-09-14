package com.mockarena.question.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.Locale;

/**
 * The only executable V1 coding profile. This intentionally validates a small,
 * platform-controlled contract rather than treating arbitrary coding JSON as
 * runner input.
 */
final class CodingExecutionSpec {
    static final int SPEC_VERSION = 1;
    static final String JAVA_PROFILE = "java-21-stdio-v1";
    private static final int MAX_HIDDEN_TESTS = 100;
    private static final int MAX_TEST_VALUE_CHARS = 64 * 1024;
    private static final int MAX_COMPILE_TIMEOUT_MS = 30_000;
    private static final int MAX_EXECUTION_TIMEOUT_MS = 10_000;
    private static final int MAX_MEMORY_MB = 2_048;
    private static final int MAX_OUTPUT_BYTES = 1_048_576;

    private final JsonNode value;

    private CodingExecutionSpec(JsonNode value) { this.value = value.deepCopy(); }

    static CodingExecutionSpec parse(JsonNode input) {
        if (input == null || !input.isObject()) throw new IllegalArgumentException("Coding execution spec must be an object");
        rejectUnknown(input, Set.of("specVersion", "runtimeProfileId", "ioContract", "sourceFilename", "entrypoint", "allowedProgrammingLanguages", "comparison", "executionLimits", "scoringPolicy", "hiddenTests"));
        int version = positiveInt(input, "specVersion", Integer.MAX_VALUE);
        if (version != SPEC_VERSION) throw new IllegalArgumentException("Unsupported coding execution spec version");
        if (!JAVA_PROFILE.equals(requiredText(input, "runtimeProfileId"))) throw new IllegalArgumentException("Unsupported coding runtime profile");
        if (!"STDIN_STDOUT".equals(requiredText(input, "ioContract"))) throw new IllegalArgumentException("Unsupported coding input-output contract");
        if (!"Main.java".equals(requiredText(input, "sourceFilename")) || !"Main".equals(requiredText(input, "entrypoint"))) throw new IllegalArgumentException("Unsupported coding entrypoint");

        JsonNode languages = requiredArray(input, "allowedProgrammingLanguages");
        if (languages.size() != 1 || !"JAVA".equals(languages.get(0).asText())) throw new IllegalArgumentException("Executable V1 coding supports JAVA only");

        JsonNode comparison = requiredObject(input, "comparison");
        rejectUnknown(comparison, Set.of("mode"));
        String mode = requiredText(comparison, "mode");
        if (!"NORMALIZED_WHITESPACE".equals(mode)) throw new IllegalArgumentException("Unsupported coding comparison mode");

        JsonNode limits = requiredObject(input, "executionLimits");
        rejectUnknown(limits, Set.of("compileTimeoutMs", "executionTimeoutMs", "memoryMb", "maxOutputBytes"));
        positiveInt(limits, "compileTimeoutMs", MAX_COMPILE_TIMEOUT_MS);
        positiveInt(limits, "executionTimeoutMs", MAX_EXECUTION_TIMEOUT_MS);
        positiveInt(limits, "memoryMb", MAX_MEMORY_MB);
        positiveInt(limits, "maxOutputBytes", MAX_OUTPUT_BYTES);

        JsonNode scoring = requiredObject(input, "scoringPolicy");
        rejectUnknown(scoring, Set.of("policyCode", "maxPoints"));
        if (!"ALL_OR_NOTHING".equals(requiredText(scoring, "policyCode"))) throw new IllegalArgumentException("Unsupported coding scoring policy");
        positiveInt(scoring, "maxPoints", Integer.MAX_VALUE);

        JsonNode hiddenTests = requiredArray(input, "hiddenTests");
        if (hiddenTests.isEmpty() || hiddenTests.size() > MAX_HIDDEN_TESTS) throw new IllegalArgumentException("Invalid hidden test count");
        for (JsonNode test : hiddenTests) {
            if (!test.isObject()) throw new IllegalArgumentException("Hidden test must be an object");
            rejectUnknown(test, Set.of("input", "output"));
            boundedText(test, "input", MAX_TEST_VALUE_CHARS);
            boundedText(test, "output", MAX_TEST_VALUE_CHARS);
        }
        return new CodingExecutionSpec(input);
    }

    JsonNode canonical() { return value.deepCopy(); }
    void requireMatchingSupportedLanguages(JsonNode languages) {
        if (languages == null || !languages.isArray() || languages.size() != 1 || !"JAVA".equals(languages.get(0).asText())) {
            throw new IllegalArgumentException("Executable coding supported languages must match the runtime profile");
        }
    }
    JsonNode hiddenTests() { return value.required("hiddenTests").deepCopy(); }
    JsonNode executionLimits() { return value.required("executionLimits").deepCopy(); }
    JsonNode scoringPolicy() { return value.required("scoringPolicy").deepCopy(); }
    JsonNode allowedProgrammingLanguages() { return value.required("allowedProgrammingLanguages").deepCopy(); }
    String runtimeProfileId() { return value.required("runtimeProfileId").asText(); }
    JsonNode testSpecification() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("specVersion", value.required("specVersion").asInt());
        result.put("ioContract", value.required("ioContract").asText());
        result.set("comparison", value.required("comparison").deepCopy());
        result.set("hiddenTests", hiddenTests());
        return result;
    }

    private static JsonNode requiredObject(JsonNode node, String name) { JsonNode value = node.get(name); if (value == null || !value.isObject()) throw new IllegalArgumentException("Coding execution spec requires " + name); return value; }
    private static JsonNode requiredArray(JsonNode node, String name) { JsonNode value = node.get(name); if (value == null || !value.isArray()) throw new IllegalArgumentException("Coding execution spec requires " + name); return value; }
    private static String requiredText(JsonNode node, String name) { JsonNode value = node.get(name); if (value == null || !value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("Coding execution spec requires " + name); return value.asText().trim().toUpperCase(Locale.ROOT).equals("JAVA") ? "JAVA" : value.asText().trim(); }
    private static int positiveInt(JsonNode node, String name, int max) { JsonNode value=node.get(name); if(value==null || !value.canConvertToInt() || value.asInt()<=0 || value.asInt()>max) throw new IllegalArgumentException("Invalid coding execution spec " + name); return value.asInt(); }
    private static void boundedText(JsonNode node,String name,int max){JsonNode value=node.get(name);if(value==null||!value.isTextual()||value.asText().isEmpty()||value.asText().length()>max)throw new IllegalArgumentException("Invalid hidden test "+name);}
    private static void rejectUnknown(JsonNode node, Set<String> allowed) { node.fieldNames().forEachRemaining(name -> { if (!allowed.contains(name)) throw new IllegalArgumentException("Unsupported coding execution spec field"); }); }
}
