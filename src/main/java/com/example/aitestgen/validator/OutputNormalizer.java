package com.example.aitestgen.validator;

import com.example.aitestgen.model.TestCaseResponse;
import com.example.aitestgen.model.TestScenario;

import java.util.*;

/**
 * Module 6: Output Normalizer
 *
 * Cleans and standardizes the parsed TestCaseResponse object:
 *
 * 1. Removes duplicate test cases (by test_case_id)
 * 2. Standardizes priority values to: High, Medium, Low (title case)
 * 3. Standardizes type values to allowed enum values
 * 4. Reassigns sequential test_case_id values (TC_001, TC_002...)
 * 5. Ensures minimum required fields are present
 * 6. Fills defaults for missing optional fields
 */
public class OutputNormalizer {

    // Allowed priority values (canonical form)
    private static final Set<String> VALID_PRIORITIES = Set.of("High", "Medium", "Low");

    // Allowed type values (canonical form)
    private static final Set<String> VALID_TYPES = Set.of(
            "Functional", "Negative", "Boundary", "Validation", "Error Handling"
    );

    /**
     * Normalizes the full TestCaseResponse object.
     *
     * @param response The raw parsed TestCaseResponse from the LLM.
     * @return Cleaned and normalized TestCaseResponse.
     */
    public TestCaseResponse normalize(TestCaseResponse response) {
        System.out.println("[OutputNormalizer] Starting normalization...");

        if (response == null) {
            throw new IllegalArgumentException("[OutputNormalizer] Response cannot be null.");
        }

        // Normalize top-level fields
        response.setFeatureName(normalizeString(response.getFeatureName(), "Unknown Feature"));
        response.setRequirementSummary(normalizeString(response.getRequirementSummary(), "No summary provided."));

        // Normalize assumptions list
        if (response.getAssumptions() == null) {
            response.setAssumptions(new ArrayList<>());
        }

        // Normalize test scenarios
        if (response.getTestScenarios() != null && !response.getTestScenarios().isEmpty()) {
            List<TestScenario> normalized = normalizeScenarios(response.getTestScenarios());
            response.setTestScenarios(normalized);
            System.out.println("[OutputNormalizer] Normalized " + normalized.size() + " test scenarios.");
        } else {
            System.out.println("[OutputNormalizer] WARNING: No test scenarios found to normalize.");
            response.setTestScenarios(new ArrayList<>());
        }

        return response;
    }

    /**
     * Normalizes the list of test scenarios:
     * - Removes duplicates by title and test_case_id
     * - Standardizes priority and type
     * - Reassigns sequential IDs
     * - Ensures required fields exist
     */
    private List<TestScenario> normalizeScenarios(List<TestScenario> scenarios) {
        // Step 1: Remove duplicates by title (case-insensitive)
        List<TestScenario> deduplicated = removeDuplicates(scenarios);

        // Step 2: Normalize each scenario
        List<TestScenario> normalized = new ArrayList<>();
        for (int i = 0; i < deduplicated.size(); i++) {
            TestScenario scenario = deduplicated.get(i);
            scenario = normalizeScenario(scenario, i + 1);
            normalized.add(scenario);
        }

        return normalized;
    }

    /**
     * Removes duplicate test scenarios based on title similarity.
     */
    private List<TestScenario> removeDuplicates(List<TestScenario> scenarios) {
        Set<String> seenTitles = new LinkedHashSet<>();
        List<TestScenario> unique = new ArrayList<>();

        for (TestScenario scenario : scenarios) {
            String titleKey = (scenario.getTitle() != null)
                    ? scenario.getTitle().toLowerCase().trim()
                    : "untitled_" + UUID.randomUUID();

            if (seenTitles.add(titleKey)) {
                unique.add(scenario);
            } else {
                System.out.println("[OutputNormalizer] Removed duplicate scenario: " + scenario.getTitle());
            }
        }

        return unique;
    }

    /**
     * Normalizes a single TestScenario object.
     *
     * @param scenario The scenario to normalize.
     * @param index    Position index (1-based) used for generating test_case_id.
     * @return Normalized scenario.
     */
    private TestScenario normalizeScenario(TestScenario scenario, int index) {
        // Reassign sequential test_case_id
        scenario.setTestCaseId(String.format("TC_%03d", index));

        // Normalize title
        scenario.setTitle(normalizeString(scenario.getTitle(), "Untitled Test Case"));

        // Normalize objective
        scenario.setObjective(normalizeString(scenario.getObjective(), "Verify expected behavior."));

        // Normalize preconditions
        if (scenario.getPreconditions() == null || scenario.getPreconditions().isEmpty()) {
            scenario.setPreconditions(List.of("Application is accessible and running."));
        }

        // Normalize test_steps — ensure minimum 3 steps
        if (scenario.getTestSteps() == null || scenario.getTestSteps().size() < 3) {
            List<String> steps = new ArrayList<>(
                    scenario.getTestSteps() != null ? scenario.getTestSteps() : new ArrayList<>()
            );
            while (steps.size() < 3) {
                steps.add("Step " + (steps.size() + 1) + ": Execute required action.");
            }
            scenario.setTestSteps(steps);
        }

        // Normalize expected_result
        scenario.setExpectedResult(
                normalizeString(scenario.getExpectedResult(), "System behaves as expected.")
        );

        // Normalize priority — default to Medium if invalid
        scenario.setPriority(normalizePriority(scenario.getPriority()));

        // Normalize type — default to Functional if invalid
        scenario.setType(normalizeType(scenario.getType()));

        // Ensure test_data exists
        if (scenario.getTestData() == null) {
            scenario.setTestData(new com.example.aitestgen.model.TestData("N/A", "No specific test data."));
        }

        return scenario;
    }

    /**
     * Normalizes priority value to title case.
     * Falls back to "Medium" if the value is unrecognized.
     */
    private String normalizePriority(String priority) {
        if (priority == null) return "Medium";

        // Try title-case match first
        String titleCased = toTitleCase(priority.trim());
        if (VALID_PRIORITIES.contains(titleCased)) {
            return titleCased;
        }

        // Try case-insensitive match
        for (String valid : VALID_PRIORITIES) {
            if (valid.equalsIgnoreCase(priority.trim())) {
                return valid;
            }
        }

        System.out.println("[OutputNormalizer] Invalid priority '" + priority + "' — defaulting to 'Medium'.");
        return "Medium";
    }

    /**
     * Normalizes type value.
     * Falls back to "Functional" if the value is unrecognized.
     */
    private String normalizeType(String type) {
        if (type == null) return "Functional";

        // Direct match
        if (VALID_TYPES.contains(type.trim())) {
            return type.trim();
        }

        // Case-insensitive match
        for (String valid : VALID_TYPES) {
            if (valid.equalsIgnoreCase(type.trim())) {
                return valid;
            }
        }

        System.out.println("[OutputNormalizer] Invalid type '" + type + "' — defaulting to 'Functional'.");
        return "Functional";
    }

    /**
     * Returns a normalized string — uses the default value if the input is null/blank.
     */
    private String normalizeString(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    /**
     * Converts a string to title case (first letter uppercase, rest lowercase).
     */
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }
}
