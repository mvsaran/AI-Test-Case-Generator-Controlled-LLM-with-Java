package com.example.aitestgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Root model class representing the full test case generation response.
 * This maps directly to the top-level JSON structure defined in testcase-schema.json.
 *
 * Structure:
 * {
 *   "feature_name": "...",
 *   "requirement_summary": "...",
 *   "assumptions": [...],
 *   "test_scenarios": [...]
 * }
 */
public class TestCaseResponse {

    /**
     * Name of the feature being tested.
     * Example: "User Login"
     */
    @JsonProperty("feature_name")
    private String featureName;

    /**
     * Short human-readable summary of the requirement.
     */
    @JsonProperty("requirement_summary")
    private String requirementSummary;

    /**
     * List of assumptions made by the AI while generating test cases.
     */
    @JsonProperty("assumptions")
    private List<String> assumptions;

    /**
     * The main list of generated test scenarios.
     * Must contain at least 3 scenarios as per schema constraints.
     */
    @JsonProperty("test_scenarios")
    private List<TestScenario> testScenarios;

    // =========================================================
    // Constructors
    // =========================================================

    public TestCaseResponse() {
        // Default constructor required by Jackson
    }

    // =========================================================
    // Getters and Setters
    // =========================================================

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getRequirementSummary() {
        return requirementSummary;
    }

    public void setRequirementSummary(String requirementSummary) {
        this.requirementSummary = requirementSummary;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }

    public List<TestScenario> getTestScenarios() {
        return testScenarios;
    }

    public void setTestScenarios(List<TestScenario> testScenarios) {
        this.testScenarios = testScenarios;
    }

    /**
     * Helper: count negative scenarios.
     */
    public long countNegativeScenarios() {
        if (testScenarios == null) return 0;
        return testScenarios.stream().filter(TestScenario::isNegative).count();
    }

    /**
     * Helper: count scenarios by priority.
     */
    public long countByPriority(String priority) {
        if (testScenarios == null) return 0;
        return testScenarios.stream()
                .filter(s -> priority.equalsIgnoreCase(s.getPriority()))
                .count();
    }

    /**
     * Helper: count automation candidates.
     */
    public long countAutomationCandidates() {
        if (testScenarios == null) return 0;
        return testScenarios.stream().filter(TestScenario::isAutomationCandidate).count();
    }

    @Override
    public String toString() {
        return "TestCaseResponse{" +
                "featureName='" + featureName + '\'' +
                ", requirementSummary='" + requirementSummary + '\'' +
                ", totalScenarios=" + (testScenarios != null ? testScenarios.size() : 0) +
                '}';
    }
}
