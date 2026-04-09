package com.example.aitestgen;

import com.example.aitestgen.model.TestCaseResponse;
import com.example.aitestgen.model.TestData;
import com.example.aitestgen.model.TestScenario;
import com.example.aitestgen.parser.ResponseParser;
import com.example.aitestgen.validator.OutputNormalizer;
import com.example.aitestgen.validator.SchemaValidator;
import com.example.aitestgen.validator.SchemaValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for the full generation flow:
 * Parser → Validator → Normalizer
 *
 * Simulates what happens when the LLM returns a response:
 * - Parsing the JSON
 * - Validating against schema
 * - Normalizing the response
 */
@DisplayName("Generator Flow Tests")
class GeneratorFlowTest {

    private ResponseParser parser;
    private SchemaValidator validator;
    private OutputNormalizer normalizer;

    // A valid full JSON response
    private static final String VALID_RESPONSE_JSON = """
            {
              "feature_name": "User Login",
              "requirement_summary": "Registered users can log in using email and password.",
              "assumptions": [
                "The application is accessible via web browser",
                "Users have already registered with valid credentials"
              ],
              "test_scenarios": [
                {
                  "test_case_id": "TC_001",
                  "title": "Successful login with valid credentials",
                  "objective": "Verify that a registered user can log in successfully with valid email and password.",
                  "preconditions": ["User is registered", "Application is running", "User is on login page"],
                  "test_steps": [
                    "Step 1: Navigate to the login page",
                    "Step 2: Enter valid email address",
                    "Step 3: Enter valid password",
                    "Step 4: Click the Login button",
                    "Step 5: Verify redirection to dashboard"
                  ],
                  "test_data": {
                    "input": "email=user@example.com, password=SecurePass@123",
                    "notes": "Use valid registered account credentials"
                  },
                  "expected_result": "User is redirected to the dashboard after successful login.",
                  "priority": "High",
                  "type": "Functional",
                  "negative": false,
                  "automation_candidate": true
                },
                {
                  "test_case_id": "TC_002",
                  "title": "Login fails with incorrect password",
                  "objective": "Verify that the system shows error message for invalid password.",
                  "preconditions": ["User is registered", "Application is running"],
                  "test_steps": [
                    "Step 1: Navigate to the login page",
                    "Step 2: Enter a valid registered email",
                    "Step 3: Enter an incorrect password",
                    "Step 4: Click the Login button"
                  ],
                  "test_data": {
                    "input": "email=user@example.com, password=WrongPass",
                    "notes": "Use incorrect password for a registered account"
                  },
                  "expected_result": "Error message displayed: Invalid credentials. User stays on login page.",
                  "priority": "High",
                  "type": "Negative",
                  "negative": true,
                  "automation_candidate": true
                },
                {
                  "test_case_id": "TC_003",
                  "title": "Login with empty email field",
                  "objective": "Verify that form validation prevents login when email is empty.",
                  "preconditions": ["Application is running", "User is on login page"],
                  "test_steps": [
                    "Step 1: Navigate to the login page",
                    "Step 2: Leave the email field blank",
                    "Step 3: Enter a valid password",
                    "Step 4: Click the Login button"
                  ],
                  "test_data": {
                    "input": "email=, password=SecurePass@123",
                    "notes": "Empty email field"
                  },
                  "expected_result": "Validation error: Email is required. Login button is disabled or error shown.",
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
        parser = new ResponseParser();
        validator = new SchemaValidator();
        normalizer = new OutputNormalizer();
    }

    // ──────────────────────────────────────────────────────────
    // Parser Tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parser should successfully parse valid JSON into TestCaseResponse")
    void parserShouldParseValidJson() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);

        assertNotNull(response, "Parsed response should not be null");
        assertEquals("User Login", response.getFeatureName());
        assertNotNull(response.getTestScenarios());
        assertEquals(3, response.getTestScenarios().size());
    }

    @Test
    @DisplayName("Parsed response should contain correct test case data")
    void parsedResponseShouldHaveCorrectData() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);

        TestScenario first = response.getTestScenarios().get(0);
        assertEquals("TC_001", first.getTestCaseId());
        assertEquals("Successful login with valid credentials", first.getTitle());
        assertEquals("High", first.getPriority());
        assertEquals("Functional", first.getType());
        assertFalse(first.isNegative());
        assertTrue(first.isAutomationCandidate());
        assertEquals(5, first.getTestSteps().size());
    }

    @Test
    @DisplayName("Parser should throw exception for completely invalid JSON")
    void parserShouldThrowForInvalidJson() {
        assertThrows(Exception.class, () -> parser.parse("not a json string"),
                "Parser should throw exception for invalid JSON string");
    }

    @Test
    @DisplayName("isValidJson should return true for valid JSON")
    void isValidJsonShouldReturnTrueForValidJson() {
        assertTrue(parser.isValidJson(VALID_RESPONSE_JSON));
    }

    @Test
    @DisplayName("isValidJson should return false for invalid JSON")
    void isValidJsonShouldReturnFalseForInvalidJson() {
        assertFalse(parser.isValidJson("just text, not json"));
    }

    // ──────────────────────────────────────────────────────────
    // Normalizer Tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Normalizer should reassign sequential test_case_ids")
    void normalizerShouldReassignIds() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);
        // Manually mess up the IDs
        response.getTestScenarios().get(0).setTestCaseId("RANDOM_1");
        response.getTestScenarios().get(1).setTestCaseId("RANDOM_2");
        response.getTestScenarios().get(2).setTestCaseId("RANDOM_3");

        TestCaseResponse normalized = normalizer.normalize(response);

        assertEquals("TC_001", normalized.getTestScenarios().get(0).getTestCaseId());
        assertEquals("TC_002", normalized.getTestScenarios().get(1).getTestCaseId());
        assertEquals("TC_003", normalized.getTestScenarios().get(2).getTestCaseId());
    }

    @Test
    @DisplayName("Normalizer should remove duplicate scenarios by title")
    void normalizerShouldRemoveDuplicates() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);

        // Add a duplicate of the first scenario
        TestScenario duplicate = new TestScenario();
        duplicate.setTestCaseId("TC_999");
        duplicate.setTitle("Successful login with valid credentials"); // Same title as TC_001
        duplicate.setObjective("Duplicate scenario");
        duplicate.setPreconditions(List.of("App running"));
        duplicate.setTestSteps(List.of("Step 1", "Step 2", "Step 3"));
        duplicate.setTestData(new TestData("input", "notes"));
        duplicate.setExpectedResult("Some result");
        duplicate.setPriority("Low");
        duplicate.setType("Functional");

        List<TestScenario> scenarios = new ArrayList<>(response.getTestScenarios());
        scenarios.add(duplicate);
        response.setTestScenarios(scenarios);

        assertEquals(4, response.getTestScenarios().size(), "Before normalization: 4 scenarios");

        TestCaseResponse normalized = normalizer.normalize(response);

        assertEquals(3, normalized.getTestScenarios().size(),
                "After normalization: duplicate should be removed, leaving 3 scenarios");
    }

    @Test
    @DisplayName("Normalizer should fix invalid priority to 'Medium'")
    void normalizerShouldFixInvalidPriority() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);
        response.getTestScenarios().get(0).setPriority("INVALID_PRIORITY");

        TestCaseResponse normalized = normalizer.normalize(response);

        assertEquals("Medium", normalized.getTestScenarios().get(0).getPriority(),
                "Invalid priority should default to Medium");
    }

    @Test
    @DisplayName("Normalizer should fix invalid type to 'Functional'")
    void normalizerShouldFixInvalidType() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);
        response.getTestScenarios().get(0).setType("INVALID_TYPE");

        TestCaseResponse normalized = normalizer.normalize(response);

        assertEquals("Functional", normalized.getTestScenarios().get(0).getType(),
                "Invalid type should default to Functional");
    }

    @Test
    @DisplayName("Normalizer should pad test_steps to minimum 3 if insufficient")
    void normalizerShouldPadTestSteps() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);
        // Set only 1 test step
        response.getTestScenarios().get(0).setTestSteps(List.of("Step 1: Only step"));

        TestCaseResponse normalized = normalizer.normalize(response);

        assertTrue(normalized.getTestScenarios().get(0).getTestSteps().size() >= 3,
                "Normalizer should pad test_steps to at least 3 steps");
    }

    // ──────────────────────────────────────────────────────────
    // Full Flow Integration Tests
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full flow: parse → validate → normalize should work for valid JSON")
    void fullFlowShouldSucceedForValidJson() throws Exception {
        // Step 1: Parse
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);
        assertNotNull(response);

        // Step 2: Validate
        ValidationResult validationResult = validator.validate(VALID_RESPONSE_JSON);
        assertTrue(validationResult.isPassed(), "Valid JSON should pass schema validation.");

        // Step 3: Normalize
        TestCaseResponse normalized = normalizer.normalize(response);
        assertNotNull(normalized);
        assertFalse(normalized.getTestScenarios().isEmpty());

        // Step 4: Count helpers
        assertEquals(1, normalized.countNegativeScenarios() >= 1 ? 1 : 0,
                "Should have at least 1 negative scenario");
    }

    @Test
    @DisplayName("TestCaseResponse helpers should count correctly")
    void testCaseResponseHelpersShouldCountCorrectly() throws Exception {
        TestCaseResponse response = parser.parse(VALID_RESPONSE_JSON);

        // 2 negative scenarios (TC_002 and TC_003)
        assertEquals(2, response.countNegativeScenarios());

        // 2 High priority
        assertEquals(2, response.countByPriority("High"));

        // 1 Medium priority
        assertEquals(1, response.countByPriority("Medium"));

        // 3 automation candidates
        assertEquals(3, response.countAutomationCandidates());
    }
}
