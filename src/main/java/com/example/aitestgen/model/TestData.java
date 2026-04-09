package com.example.aitestgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model representing test data associated with a test case.
 * Maps to the "test_data" field in the JSON schema.
 */
public class TestData {

    /**
     * The actual input data used during test execution.
     * Example: "email=test@example.com, password=Test@123"
     */
    @JsonProperty("input")
    private String input;

    /**
     * Additional notes or context about the test data.
     * Example: "Valid registered user credentials"
     */
    @JsonProperty("notes")
    private String notes;

    // =========================================================
    // Constructors
    // =========================================================

    public TestData() {
        // Default constructor required by Jackson
    }

    public TestData(String input, String notes) {
        this.input = input;
        this.notes = notes;
    }

    // =========================================================
    // Getters and Setters
    // =========================================================

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "TestData{" +
                "input='" + input + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
