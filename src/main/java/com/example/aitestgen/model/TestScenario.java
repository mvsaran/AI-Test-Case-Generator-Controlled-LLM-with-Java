package com.example.aitestgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Model representing a single test scenario (test case).
 * Maps to each item in the "test_scenarios" array in the JSON schema.
 *
 * Allowed Priority values: High, Medium, Low
 * Allowed Type values: Functional, Negative, Boundary, Validation, Error Handling
 */
public class TestScenario {

    /**
     * Unique identifier for the test case. Format: TC_001, TC_002, etc.
     */
    @JsonProperty("test_case_id")
    private String testCaseId;

    /**
     * Descriptive title of the test case.
     */
    @JsonProperty("title")
    private String title;

    /**
     * What this test case intends to verify.
     */
    @JsonProperty("objective")
    private String objective;

    /**
     * List of conditions that must be met before test execution.
     */
    @JsonProperty("preconditions")
    private List<String> preconditions;

    /**
     * Ordered list of steps to execute this test. Minimum 3 steps required.
     */
    @JsonProperty("test_steps")
    private List<String> testSteps;

    /**
     * Input data and notes for the test.
     */
    @JsonProperty("test_data")
    private TestData testData;

    /**
     * The expected outcome after executing all test steps.
     */
    @JsonProperty("expected_result")
    private String expectedResult;

    /**
     * Priority of this test case: High, Medium, or Low.
     */
    @JsonProperty("priority")
    private String priority;

    /**
     * Type of test: Functional, Negative, Boundary, Validation, or Error Handling.
     */
    @JsonProperty("type")
    private String type;

    /**
     * True if this is a negative test case (testing invalid/error conditions).
     */
    @JsonProperty("negative")
    private boolean negative;

    /**
     * True if this test case can be automated using Selenium or similar tools.
     */
    @JsonProperty("automation_candidate")
    private boolean automationCandidate;

    // =========================================================
    // Constructors
    // =========================================================

    public TestScenario() {
        // Default constructor required by Jackson
    }

    // =========================================================
    // Getters and Setters
    // =========================================================

    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public List<String> getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(List<String> preconditions) {
        this.preconditions = preconditions;
    }

    public List<String> getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(List<String> testSteps) {
        this.testSteps = testSteps;
    }

    public TestData getTestData() {
        return testData;
    }

    public void setTestData(TestData testData) {
        this.testData = testData;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public boolean isAutomationCandidate() {
        return automationCandidate;
    }

    public void setAutomationCandidate(boolean automationCandidate) {
        this.automationCandidate = automationCandidate;
    }

    @Override
    public String toString() {
        return "TestScenario{" +
                "testCaseId='" + testCaseId + '\'' +
                ", title='" + title + '\'' +
                ", priority='" + priority + '\'' +
                ", type='" + type + '\'' +
                ", negative=" + negative +
                ", automationCandidate=" + automationCandidate +
                '}';
    }
}
