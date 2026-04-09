package com.example.aitestgen.parser;

import com.example.aitestgen.model.TestCaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * Module 4: Response Parser
 *
 * Parses the raw JSON string returned by the OpenAI API into
 * strongly-typed Java model objects (TestCaseResponse, TestScenario, TestData).
 *
 * Uses Jackson ObjectMapper for deserialization.
 */
public class ResponseParser {

    private final ObjectMapper objectMapper;

    public ResponseParser() {
        this.objectMapper = new ObjectMapper();

        // Fail if the JSON has unknown fields not in our model
        // (Helps catch schema drift early)
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Parses a raw JSON string into a TestCaseResponse object.
     *
     * @param jsonString The raw JSON response from the LLM.
     * @return Parsed TestCaseResponse object.
     * @throws Exception If JSON parsing fails or is malformed.
     */
    public TestCaseResponse parse(String jsonString) throws Exception {
        System.out.println("[ResponseParser] Attempting to parse LLM JSON response...");

        if (jsonString == null || jsonString.isBlank()) {
            throw new IllegalArgumentException("[ResponseParser] JSON string is null or empty.");
        }

        try {
            TestCaseResponse response = objectMapper.readValue(jsonString, TestCaseResponse.class);
            System.out.println("[ResponseParser] Successfully parsed "
                    + (response.getTestScenarios() != null ? response.getTestScenarios().size() : 0)
                    + " test scenarios.");
            return response;
        } catch (Exception e) {
            throw new Exception("[ResponseParser] Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a TestCaseResponse object back to a formatted JSON string.
     * Used for writing the final output to file.
     *
     * @param response The TestCaseResponse object to serialize.
     * @return Pretty-printed JSON string.
     * @throws Exception If serialization fails.
     */
    public String toJson(TestCaseResponse response) throws Exception {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }

    /**
     * Checks if a string is valid JSON (basic check before full parsing).
     *
     * @param jsonString The string to check.
     * @return true if parseable as JSON, false otherwise.
     */
    public boolean isValidJson(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
