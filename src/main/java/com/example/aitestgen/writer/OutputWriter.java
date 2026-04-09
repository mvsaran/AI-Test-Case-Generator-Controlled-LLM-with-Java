package com.example.aitestgen.writer;

import com.example.aitestgen.model.TestCaseResponse;
import com.example.aitestgen.model.TestScenario;
import com.example.aitestgen.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Module 7: Output Writer
 *
 * Handles writing the final generated test cases to the output/ folder.
 *
 * Writes:
 * 1. generated_testcases.json — The full validated, normalized JSON output
 * 2. summary.txt — A human-readable summary of what was generated
 * 3. raw_response_debug.txt — Only written if the LLM response was invalid/malformed
 */
public class OutputWriter {

    private static final String OUTPUT_DIR = "output";
    private static final String JSON_OUTPUT_FILE = OUTPUT_DIR + "/generated_testcases.json";
    private static final String CSV_OUTPUT_FILE = OUTPUT_DIR + "/generated_testcases.csv";
    private static final String SUMMARY_FILE = OUTPUT_DIR + "/summary.txt";
    private static final String DEBUG_FILE = OUTPUT_DIR + "/raw_response_debug.txt";

    private final ObjectMapper objectMapper;

    public OutputWriter() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Writes the final validated and normalized TestCaseResponse to:
     * 1. output/generated_testcases.json (pretty-printed JSON)
     * 2. output/summary.txt (human-readable summary)
     *
     * @param response          The final normalized TestCaseResponse.
     * @param requirementSource The name of the source requirement file (for summary).
     * @throws Exception If file writing fails.
     */
    public void writeOutput(TestCaseResponse response, String requirementSource) throws Exception {
        System.out.println("[OutputWriter] Writing output files to: " + OUTPUT_DIR + "/");

        // Ensure output directory exists
        FileUtils.ensureDirectoryExists(OUTPUT_DIR);

        // 1. Write JSON output
        writeJsonOutput(response);

        // 2. Write CSV output
        writeCsvOutput(response);

        // 3. Write summary text
        writeSummary(response, requirementSource);

        System.out.println("[OutputWriter] SUCCESS: Output files written successfully.");
        System.out.println("[OutputWriter] JSON    -> " + JSON_OUTPUT_FILE);
        System.out.println("[OutputWriter] CSV     -> " + CSV_OUTPUT_FILE);
        System.out.println("[OutputWriter] Summary -> " + SUMMARY_FILE);
    }

    /**
     * Writes the raw LLM response to output/raw_response_debug.txt.
     * This is called when validation fails and the response could not be fixed.
     *
     * @param rawResponse The raw string response from the LLM.
     * @throws Exception If file writing fails.
     */
    public void writeDebugOutput(String rawResponse) throws Exception {
        FileUtils.ensureDirectoryExists(OUTPUT_DIR);
        String content = "=== RAW LLM RESPONSE (DEBUG) ===\n"
                + "Timestamp: " + getCurrentTimestamp() + "\n\n"
                + rawResponse;
        FileUtils.writeFile(DEBUG_FILE, content);
        System.out.println("[OutputWriter] WARNING: Debug response written to: " + DEBUG_FILE);
    }

    /**
     * Writes the TestCaseResponse as pretty-printed JSON.
     */
    private void writeJsonOutput(TestCaseResponse response) throws Exception {
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        FileUtils.writeFile(JSON_OUTPUT_FILE, prettyJson);
    }

    /**
     * Writes the TestCaseResponse as an aligned CSV for easy Excel viewing.
     */
    private void writeCsvOutput(TestCaseResponse response) throws Exception {
        StringBuilder csv = new StringBuilder();
        // CSV Header
        csv.append("Test Case ID,Title,Objective,Priority,Type,Negative,Auto Candidate,Preconditions,Test Steps,Test Data,Expected Result\n");

        if (response.getTestScenarios() != null) {
            for (TestScenario ts : response.getTestScenarios()) {
                String preconditions = ts.getPreconditions() != null ? String.join("\n", ts.getPreconditions()) : "";
                
                // For test steps, enumerate them clearly
                StringBuilder stepsBuilder = new StringBuilder();
                if (ts.getTestSteps() != null) {
                    for (int i = 0; i < ts.getTestSteps().size(); i++) {
                        stepsBuilder.append(i + 1).append(". ").append(ts.getTestSteps().get(i)).append("\n");
                    }
                }
                String testSteps = stepsBuilder.toString().trim();
                
                String testData = "";
                if (ts.getTestData() != null) {
                    testData = "Input: " + (ts.getTestData().getInput() != null ? ts.getTestData().getInput() : "None") + "\n" +
                               "Notes: " + (ts.getTestData().getNotes() != null ? ts.getTestData().getNotes() : "None");
                }

                csv.append(escapeCsv(ts.getTestCaseId())).append(",")
                   .append(escapeCsv(ts.getTitle())).append(",")
                   .append(escapeCsv(ts.getObjective())).append(",")
                   .append(escapeCsv(ts.getPriority())).append(",")
                   .append(escapeCsv(ts.getType())).append(",")
                   .append(ts.isNegative() ? "Yes" : "No").append(",")
                   .append(ts.isAutomationCandidate() ? "Yes" : "No").append(",")
                   .append(escapeCsv(preconditions)).append(",")
                   .append(escapeCsv(testSteps)).append(",")
                   .append(escapeCsv(testData)).append(",")
                   .append(escapeCsv(ts.getExpectedResult())).append("\n");
            }
        }
        FileUtils.writeFile(CSV_OUTPUT_FILE, csv.toString());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Escape quotes by doubling them
        String escaped = value.replace("\"", "\"\"");
        // If it contains commas, quotes, or newlines, wrap in quotes
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Writes a human-readable summary of the generated test cases.
     */
    private void writeSummary(TestCaseResponse response, String requirementSource) throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append("=".repeat(60)).append("\n");
        sb.append("   AI TEST CASE GENERATOR — SUMMARY REPORT\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append("Generated At   : ").append(getCurrentTimestamp()).append("\n");
        sb.append("Source File    : ").append(requirementSource).append("\n");
        sb.append("Feature Name   : ").append(response.getFeatureName()).append("\n\n");

        sb.append("Requirement Summary:\n");
        sb.append("  ").append(response.getRequirementSummary()).append("\n\n");

        // Assumptions
        if (response.getAssumptions() != null && !response.getAssumptions().isEmpty()) {
            sb.append("Assumptions:\n");
            response.getAssumptions().forEach(a -> sb.append("  - ").append(a).append("\n"));
            sb.append("\n");
        }

        int total = response.getTestScenarios() != null ? response.getTestScenarios().size() : 0;
        long highCount = response.countByPriority("High");
        long mediumCount = response.countByPriority("Medium");
        long lowCount = response.countByPriority("Low");
        long negativeCount = response.countNegativeScenarios();
        long automationCount = response.countAutomationCandidates();

        sb.append("-".repeat(60)).append("\n");
        sb.append("TEST SCENARIOS SUMMARY\n");
        sb.append("-".repeat(60)).append("\n");
        sb.append(String.format("  Total Scenarios     : %d\n", total));
        sb.append(String.format("  High Priority       : %d\n", highCount));
        sb.append(String.format("  Medium Priority     : %d\n", mediumCount));
        sb.append(String.format("  Low Priority        : %d\n", lowCount));
        sb.append(String.format("  Negative Scenarios  : %d\n", negativeCount));
        sb.append(String.format("  Automation Candidates: %d\n", automationCount));
        sb.append("\n");

        // Per-scenario details
        sb.append("-".repeat(60)).append("\n");
        sb.append("TEST CASE LIST\n");
        sb.append("-".repeat(60)).append("\n");

        if (response.getTestScenarios() != null) {
            for (TestScenario scenario : response.getTestScenarios()) {
                sb.append(String.format("[%s] %s\n", scenario.getTestCaseId(), scenario.getTitle()));
                sb.append(String.format("       Priority: %-8s | Type: %-20s | Negative: %s | Auto: %s\n",
                        scenario.getPriority(),
                        scenario.getType(),
                        scenario.isNegative() ? "YES" : "NO",
                        scenario.isAutomationCandidate() ? "YES" : "NO"));
            }
        }

        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("Output JSON: ").append(JSON_OUTPUT_FILE).append("\n");
        sb.append("=".repeat(60)).append("\n");

        FileUtils.writeFile(SUMMARY_FILE, sb.toString());
    }

    /**
     * Returns a formatted timestamp for file headers.
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getJsonOutputPath() {
        return JSON_OUTPUT_FILE;
    }

    public String getSummaryPath() {
        return SUMMARY_FILE;
    }
}
