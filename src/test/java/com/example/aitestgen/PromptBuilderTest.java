package com.example.aitestgen;

import com.example.aitestgen.prompt.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PromptBuilder module.
 *
 * Tests verify that:
 * - System prompt contains all required guardrail instructions
 * - User prompt includes the requirement text
 * - User prompt enforces JSON-only output
 * - User prompt includes enum constraints
 * - Correction prompt includes the error summary
 */
@DisplayName("PromptBuilder Tests")
class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    @DisplayName("System prompt should not be null or empty")
    void systemPromptShouldNotBeEmpty() {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        assertNotNull(systemPrompt, "System prompt should not be null");
        assertFalse(systemPrompt.isBlank(), "System prompt should not be blank");
    }

    @Test
    @DisplayName("System prompt should enforce JSON-only output")
    void systemPromptShouldEnforceJsonOutput() {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        assertTrue(systemPrompt.toLowerCase().contains("json"),
                "System prompt must mention JSON output requirement");
        // Check for anti-markdown instruction
        assertTrue(systemPrompt.toLowerCase().contains("markdown"),
                "System prompt must mention no-markdown rule");
    }

    @Test
    @DisplayName("System prompt should require negative scenarios")
    void systemPromptShouldRequireNegativeScenarios() {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        assertTrue(systemPrompt.toLowerCase().contains("negative"),
                "System prompt must require negative scenarios");
    }

    @Test
    @DisplayName("System prompt should enforce priority enum values")
    void systemPromptShouldEnforcePriorityEnum() {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        assertTrue(systemPrompt.contains("High"), "System prompt should list High priority");
        assertTrue(systemPrompt.contains("Medium"), "System prompt should list Medium priority");
        assertTrue(systemPrompt.contains("Low"), "System prompt should list Low priority");
    }

    @Test
    @DisplayName("User prompt should contain the requirement text")
    void userPromptShouldContainRequirement() {
        String requirement = "User should be able to log in with email and password.";
        String userPrompt = promptBuilder.buildUserPrompt(requirement);

        assertNotNull(userPrompt, "User prompt should not be null");
        assertTrue(userPrompt.contains(requirement),
                "User prompt must embed the requirement text");
    }

    @Test
    @DisplayName("User prompt should specify minimum test scenarios count")
    void userPromptShouldSpecifyMinimumScenarios() {
        String userPrompt = promptBuilder.buildUserPrompt("Sample requirement.");
        // Check for the minimum scenario count instruction
        assertTrue(userPrompt.contains("minimum") || userPrompt.contains("8"),
                "User prompt should specify a minimum number of scenarios");
    }

    @Test
    @DisplayName("User prompt should enforce enum values for priority")
    void userPromptShouldEnforcePriorityEnums() {
        String userPrompt = promptBuilder.buildUserPrompt("Sample requirement.");
        assertTrue(userPrompt.contains("High"), "User prompt should include High priority");
        assertTrue(userPrompt.contains("Medium"), "User prompt should include Medium priority");
        assertTrue(userPrompt.contains("Low"), "User prompt should include Low priority");
    }

    @Test
    @DisplayName("User prompt should enforce enum values for type")
    void userPromptShouldEnforceTypeEnums() {
        String userPrompt = promptBuilder.buildUserPrompt("Sample requirement.");
        assertTrue(userPrompt.contains("Functional"), "User prompt should include Functional type");
        assertTrue(userPrompt.contains("Negative"), "User prompt should include Negative type");
        assertTrue(userPrompt.contains("Boundary"), "User prompt should include Boundary type");
        assertTrue(userPrompt.contains("Validation"), "User prompt should include Validation type");
        assertTrue(userPrompt.contains("Error Handling"), "User prompt should include Error Handling type");
    }

    @Test
    @DisplayName("Correction prompt should include the bad response and validation errors")
    void correctionPromptShouldIncludeBadResponseAndErrors() {
        String requirement = "User should be able to log in.";
        String badResponse = "{\"invalid\": true}";
        String errors = "- Missing required field: feature_name\n- Missing required field: test_scenarios";

        String correctionPrompt = promptBuilder.buildCorrectionPrompt(requirement, badResponse, errors);

        assertNotNull(correctionPrompt);
        assertTrue(correctionPrompt.contains(requirement),
                "Correction prompt must contain original requirement");
        assertTrue(correctionPrompt.contains(badResponse),
                "Correction prompt must contain previous bad response");
        assertTrue(correctionPrompt.contains("feature_name"),
                "Correction prompt must mention specific errors");
    }

    @Test
    @DisplayName("User prompt should include JSON structure template")
    void userPromptShouldIncludeJsonStructure() {
        String userPrompt = promptBuilder.buildUserPrompt("Test requirement.");
        assertTrue(userPrompt.contains("test_case_id"),
                "User prompt should include test_case_id in JSON template");
        assertTrue(userPrompt.contains("expected_result"),
                "User prompt should include expected_result in JSON template");
        assertTrue(userPrompt.contains("automation_candidate"),
                "User prompt should include automation_candidate in JSON template");
    }
}
