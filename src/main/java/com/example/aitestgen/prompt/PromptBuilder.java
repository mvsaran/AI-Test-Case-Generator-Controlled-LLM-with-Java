package com.example.aitestgen.prompt;

/**
 * Module 2: Prompt Builder
 *
 * Builds a highly controlled LLM prompt that:
 * - Forces JSON-only output (no markdown, no explanations)
 * - Instructs the LLM to generate positive, negative, and edge case scenarios
 * - Enforces enum values for priority and type
 * - Enforces minimum test steps (3+)
 * - Includes the expected JSON schema structure inline
 * - Supports a "correction prompt" for retry attempts when output is malformed
 */
public class PromptBuilder {

    /**
     * Builds the system-level prompt that establishes the LLM's persona and behavior rules.
     * This is sent as the "system" message in the OpenAI Chat API call.
     *
     * @return System prompt string.
     */
    public String buildSystemPrompt() {
        return """
                You are an expert QA test case generation assistant embedded in a Java automation framework.
                
                STRICT RULES — YOU MUST FOLLOW THESE WITHOUT EXCEPTION:
                1. Return ONLY valid JSON. No markdown. No code blocks. No triple backticks.
                2. Do NOT include any text before or after the JSON.
                3. Do NOT include any explanation, commentary, or notes outside the JSON.
                4. The JSON must strictly follow the schema provided in the user prompt.
                5. Every test case MUST have an "expected_result" field with a meaningful value.
                6. Every test case MUST have at least 3 test_steps.
                7. "priority" MUST be exactly one of: High, Medium, Low
                8. "type" MUST be exactly one of: Functional, Negative, Boundary, Validation, Error Handling
                9. "negative" MUST be a boolean: true or false
                10. "automation_candidate" MUST be a boolean: true or false
                11. Include BOTH positive AND negative test cases.
                12. Include edge cases (boundary conditions, empty inputs, special characters).
                13. All test_case_id values must be unique and follow the format: TC_001, TC_002, etc.
                14. Generate a MINIMUM of 8 test scenarios (mix of positive, negative, boundary).
                15. Do NOT generate duplicate test scenarios.
                """;
    }

    /**
     * Builds the user-level prompt using the normalized requirement text.
     * This prompt includes the full expected JSON schema, enum constraints,
     * and field-level instructions.
     *
     * @param requirementText The normalized requirement text from RequirementReader.
     * @return User prompt string.
     */
    public String buildUserPrompt(String requirementText) {
        return """
                REQUIREMENT:
                %s
                
                TASK:
                Generate structured QA test cases for the above requirement.
                
                OUTPUT FORMAT — Return ONLY this JSON structure:
                {
                  "feature_name": "Name of the feature (string)",
                  "requirement_summary": "Short one-line summary of the requirement (string)",
                  "assumptions": [
                    "assumption 1",
                    "assumption 2"
                  ],
                  "test_scenarios": [
                    {
                      "test_case_id": "TC_001",
                      "title": "Short descriptive title",
                      "objective": "What this test verifies",
                      "preconditions": [
                        "Condition 1 before test starts"
                      ],
                      "test_steps": [
                        "Step 1: Action",
                        "Step 2: Action",
                        "Step 3: Action"
                      ],
                      "test_data": {
                        "input": "Specific input data for this test",
                        "notes": "Any notes about the test data"
                      },
                      "expected_result": "Clearly stated expected outcome",
                      "priority": "High",
                      "type": "Functional",
                      "negative": false,
                      "automation_candidate": true
                    }
                  ]
                }
                
                CONSTRAINTS:
                - priority MUST be one of: High, Medium, Low
                - type MUST be one of: Functional, Negative, Boundary, Validation, Error Handling
                - test_steps must have minimum 3 items
                - test_scenarios must have minimum 8 items
                - Include at least 3 negative scenarios (negative: true)
                - Include at least 2 boundary/edge case scenarios
                - All test_case_id values must be unique
                - Do NOT repeat similar test cases
                - Return ONLY the JSON. Nothing else.
                """.formatted(requirementText);
    }

    /**
     * Builds a correction prompt used when the first LLM response is malformed or schema-invalid.
     * This prompt includes the original bad response and asks the LLM to fix it.
     *
     * @param requirementText  The original requirement text.
     * @param badJsonResponse  The invalid JSON response from the previous attempt.
     * @param validationErrors A summary of what validation errors were found.
     * @return Correction prompt string for the retry attempt.
     */
    public String buildCorrectionPrompt(String requirementText, String badJsonResponse, String validationErrors) {
        return """
                Your previous response failed JSON schema validation.
                
                ORIGINAL REQUIREMENT:
                %s
                
                YOUR PREVIOUS (INVALID) RESPONSE:
                %s
                
                VALIDATION ERRORS FOUND:
                %s
                
                FIX RULES:
                1. Return ONLY valid JSON. No markdown. No text before or after JSON.
                2. Fix all schema validation errors listed above.
                3. Ensure "priority" is one of: High, Medium, Low
                4. Ensure "type" is one of: Functional, Negative, Boundary, Validation, Error Handling
                5. Ensure every test case has at least 3 test_steps.
                6. Ensure every test case has "expected_result".
                7. Ensure all test_case_id values are unique (TC_001, TC_002...).
                8. Include at least 3 negative scenarios (negative: true).
                9. Return the corrected JSON now with NO additional text.
                """.formatted(requirementText, badJsonResponse, validationErrors);
    }
}
