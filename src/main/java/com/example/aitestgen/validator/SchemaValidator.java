package com.example.aitestgen.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Module 5: JSON Schema Validator
 *
 * Validates LLM-generated JSON against the testcase-schema.json file.
 *
 * Uses the networknt json-schema-validator library which supports
 * JSON Schema Draft-07 — the same version used in our schema file.
 *
 * Validation enforces:
 * - Required fields presence
 * - Enum values for priority and type
 * - Minimum array lengths (test_steps >= 3, test_scenarios >= 3)
 * - Correct data types (boolean, string, array, object)
 */
public class SchemaValidator {

    private static final String SCHEMA_RESOURCE_PATH = "schema/testcase-schema.json";

    private final JsonSchema jsonSchema;
    private final ObjectMapper objectMapper;

    /**
     * Constructor: Loads the JSON schema from the classpath resources.
     * The schema is loaded once and reused for all validations.
     *
     * @throws RuntimeException If the schema file cannot be found or parsed.
     */
    public SchemaValidator() {
        this.objectMapper = new ObjectMapper();

        try {
            // Load schema from classpath (src/main/resources/schema/testcase-schema.json)
            InputStream schemaStream = getClass().getClassLoader()
                    .getResourceAsStream(SCHEMA_RESOURCE_PATH);

            if (schemaStream == null) {
                throw new RuntimeException("[SchemaValidator] Schema file not found on classpath: "
                        + SCHEMA_RESOURCE_PATH);
            }

            // Build schema factory with Draft-07 support
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            this.jsonSchema = factory.getSchema(schemaStream);

            System.out.println("[SchemaValidator] Schema loaded successfully from: " + SCHEMA_RESOURCE_PATH);

        } catch (Exception e) {
            throw new RuntimeException("[SchemaValidator] Failed to load JSON schema: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a JSON string against the testcase-schema.json schema.
     *
     * @param jsonString The JSON string to validate (should be LLM output).
     * @return ValidationResult with a pass/fail flag and list of error messages.
     */
    public ValidationResult validate(String jsonString) {
        System.out.println("[SchemaValidator] Validating JSON against schema...");

        // Guard: reject null or blank input immediately
        if (jsonString == null || jsonString.isBlank()) {
            System.out.println("[SchemaValidator] FAIL: Validation FAILED - input is null or empty.");
            ValidationMessage msg = ValidationMessage.builder()
                    .message("JSON input is null or empty")
                    .build();
            return new ValidationResult(false, Set.of(msg));
        }

        try {
            // Parse JSON string to JsonNode
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // Run schema validation
            Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);

            if (errors.isEmpty()) {
                System.out.println("[SchemaValidator] PASS: Validation PASSED - JSON is schema-compliant.");
                return new ValidationResult(true, Set.of());
            } else {
                System.out.println("[SchemaValidator] FAIL: Validation FAILED - " + errors.size() + " error(s) found.");
                errors.forEach(e -> System.out.println("   → " + e.getMessage()));
                return new ValidationResult(false, errors);
            }

        } catch (Exception e) {
            System.out.println("[SchemaValidator] FAIL: JSON parsing failed: " + e.getMessage());
            // Return failure with a synthetic error message
            ValidationMessage msg = ValidationMessage.builder()
                    .message("JSON parse error: " + e.getMessage())
                    .build();
            return new ValidationResult(false, Set.of(msg));
        }
    }

    /**
     * Inner class representing the result of a schema validation.
     */
    public static class ValidationResult {

        private final boolean passed;
        private final Set<ValidationMessage> errors;

        public ValidationResult(boolean passed, Set<ValidationMessage> errors) {
            this.passed = passed;
            this.errors = errors;
        }

        public boolean isPassed() {
            return passed;
        }

        public Set<ValidationMessage> getErrors() {
            return errors;
        }

        /**
         * Returns all error messages as a single formatted string.
         * Useful for logging and the correction prompt.
         */
        public String getErrorSummary() {
            if (errors == null || errors.isEmpty()) {
                return "No errors";
            }
            return errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("\n- ", "- ", ""));
        }
    }
}
