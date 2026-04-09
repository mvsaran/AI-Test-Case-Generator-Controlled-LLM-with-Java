package com.example.aitestgen;

import com.example.aitestgen.validator.SchemaValidator;
import com.example.aitestgen.validator.SchemaValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SchemaValidator module.
 *
 * Tests verify that:
 * - Valid JSON passes schema validation
 * - Missing required fields cause validation failure
 * - Invalid enum values for priority/type are caught
 * - Insufficient test_steps (< 3) are rejected
 * - testcase-schema.json loads correctly from classpath
 */
@DisplayName("SchemaValidator Tests")
class SchemaValidatorTest {

    private SchemaValidator validator;

    // A fully valid JSON that should pass all validations
    private static final String VALID_JSON = """
            {
              "feature_name": "User Login",
              "requirement_summary": "User should be able to log in with valid email and password.",
              "assumptions": ["Browser is supported", "User has valid credentials"],
              "test_scenarios": [
                {
                  "test_case_id": "TC_001",
                  "title": "Successful login with valid credentials",
                  "objective": "Verify that a registered user can log in successfully.",
                  "preconditions": ["User is registered", "Application is running"],
                  "test_steps": [
                    "Step 1: Navigate to the login page",
                    "Step 2: Enter valid email and password",
                    "Step 3: Click the Login button"
                  ],
                  "test_data": {
                    "input": "email=test@example.com, password=Test@123",
                    "notes": "Use a valid registered account"
                  },
                  "expected_result": "User is redirected to the dashboard after successful login.",
                  "priority": "High",
                  "type": "Functional",
                  "negative": false,
                  "automation_candidate": true
                },
                {
                  "test_case_id": "TC_002",
                  "title": "Login with invalid password",
                  "objective": "Verify that login fails with incorrect password.",
                  "preconditions": ["User is registered"],
                  "test_steps": [
                    "Step 1: Navigate to the login page",
                    "Step 2: Enter valid email and incorrect password",
                    "Step 3: Click the Login button"
                  ],
                  "test_data": {
                    "input": "email=test@example.com, password=wrongpass",
                    "notes": "Use incorrect password"
                  },
                  "expected_result": "Error message is displayed: Invalid credentials.",
                  "priority": "High",
                  "type": "Negative",
                  "negative": true,
                  "automation_candidate": true
                },
                {
                  "test_case_id": "TC_003",
                  "title": "Login with empty email field",
                  "objective": "Verify that login form validates required fields.",
                  "preconditions": ["Application is running"],
                  "test_steps": [
                    "Step 1: Navigate to the login page",
                    "Step 2: Leave email field empty",
                    "Step 3: Click the Login button"
                  ],
                  "test_data": {
                    "input": "email=, password=Test@123",
                    "notes": "Empty email field"
                  },
                  "expected_result": "Validation error message is displayed for email field.",
                  "priority": "Medium",
                  "type": "Validation",
                  "negative": true,
                  "automation_candidate": true
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        // This also tests that schema file loads correctly from classpath
        validator = new SchemaValidator();
    }

    // ──────────────────────────────────────────────────────────
    // Happy path tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid JSON should pass schema validation")
    void validJsonShouldPassValidation() {
        ValidationResult result = validator.validate(VALID_JSON);
        assertTrue(result.isPassed(), "Fully valid JSON should pass schema validation. Errors: "
                + result.getErrorSummary());
    }

    @Test
    @DisplayName("Validation result should have no errors for valid JSON")
    void validJsonShouldHaveNoErrors() {
        ValidationResult result = validator.validate(VALID_JSON);
        assertTrue(result.getErrors().isEmpty(), "Valid JSON should have no validation errors.");
    }

    // ──────────────────────────────────────────────────────────
    // Missing required fields tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Missing feature_name should fail validation")
    void missingFeatureNameShouldFail() {
        String json = """
                {
                  "requirement_summary": "Login feature",
                  "test_scenarios": []
                }
                """;
        ValidationResult result = validator.validate(json);
        assertFalse(result.isPassed(), "Missing feature_name should fail validation");
    }

    @Test
    @DisplayName("Missing requirement_summary should fail validation")
    void missingRequirementSummaryShouldFail() {
        String json = """
                {
                  "feature_name": "Login",
                  "test_scenarios": []
                }
                """;
        ValidationResult result = validator.validate(json);
        assertFalse(result.isPassed(), "Missing requirement_summary should fail validation");
    }

    @Test
    @DisplayName("Missing test_scenarios should fail validation")
    void missingTestScenariosShouldFail() {
        String json = """
                {
                  "feature_name": "Login",
                  "requirement_summary": "User should be able to log in."
                }
                """;
        ValidationResult result = validator.validate(json);
        assertFalse(result.isPassed(), "Missing test_scenarios should fail validation");
    }

    @Test
    @DisplayName("Too few test_scenarios (< 3) should fail validation")
    void tooFewTestScenariosShouldFail() {
        String json = """
                {
                  "feature_name": "Login",
                  "requirement_summary": "Login requirement.",
                  "test_scenarios": [
                    {
                      "test_case_id": "TC_001",
                      "title": "Valid login",
                      "objective": "Test login",
                      "preconditions": ["App running"],
                      "test_steps": ["Step 1", "Step 2", "Step 3"],
                      "test_data": {"input": "email", "notes": "note"},
                      "expected_result": "Login successful",
                      "priority": "High",
                      "type": "Functional",
                      "negative": false,
                      "automation_candidate": true
                    },
                    {
                      "test_case_id": "TC_002",
                      "title": "Invalid login",
                      "objective": "Test invalid login",
                      "preconditions": ["App running"],
                      "test_steps": ["Step 1", "Step 2", "Step 3"],
                      "test_data": {"input": "email", "notes": "note"},
                      "expected_result": "Error shown",
                      "priority": "High",
                      "type": "Negative",
                      "negative": true,
                      "automation_candidate": true
                    }
                  ]
                }
                """;
        ValidationResult result = validator.validate(json);
        assertFalse(result.isPassed(), "Less than 3 test_scenarios should fail validation (minItems=3)");
    }

    // ──────────────────────────────────────────────────────────
    // Enum validation tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Invalid priority value should fail validation")
    void invalidPriorityShouldFail() {
        // Replace "High" with an invalid value in the valid JSON
        String invalidJson = VALID_JSON.replace("\"priority\": \"High\"", "\"priority\": \"Critical\"")
                .replace("\"priority\": \"Medium\"", "\"priority\": \"Critical\"");
        ValidationResult result = validator.validate(invalidJson);
        assertFalse(result.isPassed(), "Invalid priority 'Critical' should fail validation");
    }

    @Test
    @DisplayName("Invalid type value should fail validation")
    void invalidTypeShouldFail() {
        String invalidJson = VALID_JSON.replace("\"type\": \"Functional\"", "\"type\": \"Smoke\"");
        ValidationResult result = validator.validate(invalidJson);
        assertFalse(result.isPassed(), "Invalid type 'Smoke' should fail validation");
    }

    // ──────────────────────────────────────────────────────────
    // test_steps minimum length tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Fewer than 3 test_steps should fail validation")
    void fewerThanThreeTestStepsShouldFail() {
        // Build a JSON directly with only 2 test_steps to avoid text-block whitespace issues
        // The schema requires minItems: 3 on test_steps, so this must fail.
        String jsonWithTwoSteps = """
                {
                  "feature_name": "Login",
                  "requirement_summary": "Login requirement for step count test.",
                  "assumptions": ["App is running"],
                  "test_scenarios": [
                    {
                      "test_case_id": "TC_001",
                      "title": "Login test with two steps",
                      "objective": "Verify login with fewer than 3 steps fails schema.",
                      "preconditions": ["App is running"],
                      "test_steps": [
                        "Step 1: Open the login page",
                        "Step 2: Click login"
                      ],
                      "test_data": { "input": "email=a@b.com", "notes": "test data" },
                      "expected_result": "User is logged in successfully.",
                      "priority": "High",
                      "type": "Functional",
                      "negative": false,
                      "automation_candidate": true
                    },
                    {
                      "test_case_id": "TC_002",
                      "title": "Another test step two",
                      "objective": "Verify another scenario for schema testing.",
                      "preconditions": ["App is running"],
                      "test_steps": [
                        "Step 1: Open the login page",
                        "Step 2: Enter password",
                        "Step 3: Click login"
                      ],
                      "test_data": { "input": "password=pass", "notes": "notes" },
                      "expected_result": "User gets an error message displayed.",
                      "priority": "High",
                      "type": "Negative",
                      "negative": true,
                      "automation_candidate": true
                    },
                    {
                      "test_case_id": "TC_003",
                      "title": "Third test with empty email",
                      "objective": "Verify empty email gives a validation error.",
                      "preconditions": ["App is running"],
                      "test_steps": [
                        "Step 1: Open the login page",
                        "Step 2: Leave email empty",
                        "Step 3: Click login"
                      ],
                      "test_data": { "input": "email=", "notes": "empty email" },
                      "expected_result": "Validation error is shown for email field.",
                      "priority": "Medium",
                      "type": "Validation",
                      "negative": true,
                      "automation_candidate": true
                    }
                  ]
                }
                """;
        ValidationResult result = validator.validate(jsonWithTwoSteps);
        assertFalse(result.isPassed(), "TC_001 has only 2 test_steps — schema minItems:3 should reject this.");
    }

    // ──────────────────────────────────────────────────────────
    // Malformed JSON tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Completely malformed JSON string should fail validation")
    void malformedJsonShouldFail() {
        String malformed = "This is not JSON at all!";
        ValidationResult result = validator.validate(malformed);
        assertFalse(result.isPassed(), "Malformed JSON should fail validation");
    }

    @Test
    @DisplayName("Empty string should fail validation")
    void emptyStringShouldFail() {
        ValidationResult result = validator.validate("");
        assertFalse(result.isPassed(), "Empty string should fail validation");
    }

    @Test
    @DisplayName("Null string should fail validation")
    void nullStringShouldFail() {
        ValidationResult result = validator.validate(null);
        assertFalse(result.isPassed(), "Null input should fail validation");
    }

    // ──────────────────────────────────────────────────────────
    // Error summary tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Error summary should be 'No errors' for valid JSON")
    void errorSummaryShouldBeEmptyForValidJson() {
        ValidationResult result = validator.validate(VALID_JSON);
        assertEquals("No errors", result.getErrorSummary());
    }

    @Test
    @DisplayName("Error summary should contain meaningful messages for invalid JSON")
    void errorSummaryShouldContainMessagesForInvalidJson() {
        String json = "{\"feature_name\": \"Login\"}";
        ValidationResult result = validator.validate(json);
        assertFalse(result.isPassed());
        assertNotNull(result.getErrorSummary());
        assertFalse(result.getErrorSummary().equals("No errors"),
                "Error summary should not be 'No errors' for invalid JSON");
    }
}
