package com.example.aitestgen;

import com.example.aitestgen.client.OpenAiClient;
import com.example.aitestgen.model.TestCaseResponse;
import com.example.aitestgen.parser.ResponseParser;
import com.example.aitestgen.prompt.PromptBuilder;
import com.example.aitestgen.reader.RequirementReader;
import com.example.aitestgen.validator.OutputNormalizer;
import com.example.aitestgen.validator.SchemaValidator;
import com.example.aitestgen.validator.SchemaValidator.ValidationResult;
import com.example.aitestgen.writer.OutputWriter;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * ============================================================
 * AI Test Case Generator — Main Entry Point
 * ============================================================
 *
 * This application:
 * 1. Reads a software requirement from a .txt file (or default)
 * 2. Builds a controlled LLM prompt (with guardrails)
 * 3. Calls the OpenAI API to generate structured test cases
 * 4. Validates the JSON output against the testcase-schema.json
 * 5. Retries up to 2 times if schema validation fails
 * 6. Normalizes and deduplicates the test scenarios
 * 7. Writes the final JSON and summary to the output/ folder
 * 8. Prints a console summary of what was generated
 *
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="com.example.aitestgen.Main"
 *   java -jar ai-test-case-generator.jar requirements/login_requirement.txt
 *
 * Configuration:
 *   Set OPENAI_API_KEY in a .env file in the project root.
 */
public class Main {

    // Maximum retry attempts if schema validation fails
    private static final int MAX_RETRIES = 2;

    // Default requirement file if no argument provided
    private static final String DEFAULT_REQUIREMENT_FILE = "requirements/login_requirement.txt";

    public static void main(String[] args) {
        printBanner();

        // ─── Step 0: Load API Key from .env ───────────────────────
        String apiKey = loadApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ OPENAI_API_KEY not found. Set it in your .env file or environment.");
            System.exit(1);
        }

        // ─── Step 1: Determine requirement file path ──────────────
        String requirementFilePath = (args.length > 0) ? args[0] : DEFAULT_REQUIREMENT_FILE;
        System.out.println("Requirement File     : " + requirementFilePath);

        // ─── Initialize all modules ───────────────────────────────
        RequirementReader reader = new RequirementReader();
        PromptBuilder promptBuilder = new PromptBuilder();
        OpenAiClient openAiClient = new OpenAiClient(apiKey);
        ResponseParser parser = new ResponseParser();
        SchemaValidator validator = new SchemaValidator();
        OutputNormalizer normalizer = new OutputNormalizer();
        OutputWriter writer = new OutputWriter();

        String rawLlmResponse = null;

        try {
            // ─── Step 2: Read and normalize the requirement ───────
            String requirementText = reader.readFromFile(requirementFilePath);
            System.out.println("SUCCESS: Requirement loaded successfully.\n");

            // ─── Step 3: Build the LLM prompt ────────────────────
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(requirementText);

            // ─── Step 4: Call OpenAI API (with retry logic) ───────
            String currentUserPrompt = userPrompt;
            ValidationResult lastValidation = null;
            TestCaseResponse finalResponse = null;

            for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
                System.out.println("\n--- Attempt " + attempt + " of " + (MAX_RETRIES + 1) + " ---");

                // Call OpenAI
                rawLlmResponse = openAiClient.callChatCompletion(systemPrompt, currentUserPrompt);

                // ─── Step 5: Validate JSON against schema ─────────
                System.out.println("\n>> Running Schema Validation...");
                lastValidation = validator.validate(rawLlmResponse);

                if (lastValidation.isPassed()) {
                    System.out.println("PASS: Schema Validation PASSED\n");

                    // ─── Step 6: Parse into Java model ────────────
                    finalResponse = parser.parse(rawLlmResponse);
                    break; // Exit retry loop

                } else {
                    System.out.println("FAIL: Schema Validation FAILED\n");
                    System.out.println("   Errors:\n" + lastValidation.getErrorSummary());

                    if (attempt <= MAX_RETRIES) {
                        System.out.println("\nRETRY: Building correction prompt for retry " + (attempt + 1) + "...");
                        // Build a correction prompt for the next attempt
                        currentUserPrompt = promptBuilder.buildCorrectionPrompt(
                                requirementText,
                                rawLlmResponse,
                                lastValidation.getErrorSummary()
                        );
                    } else {
                        // All retries exhausted — save raw response for debugging
                        System.out.println("\nWARNING: All retries exhausted. Saving raw response for debugging...");
                        writer.writeDebugOutput(rawLlmResponse);
                        System.err.println("❌ FAILED: Could not generate valid test cases after "
                                + (MAX_RETRIES + 1) + " attempts.");
                        System.exit(1);
                    }
                }
            }

            // ─── Step 7: Normalize the response ───────────────────
            if (finalResponse != null) {
                System.out.println("Processing: Normalizing test scenarios...");
                finalResponse = normalizer.normalize(finalResponse);

                // ─── Step 8: Write output files ───────────────────
                String sourceName = requirementFilePath.substring(
                        requirementFilePath.lastIndexOf('/') + 1);
                writer.writeOutput(finalResponse, sourceName);

                // ─── Step 9: Print console summary ────────────────
                printConsoleSummary(finalResponse, requirementFilePath, writer);
            }

        } catch (Exception e) {
            System.err.println("\n❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();

            // Try to save raw response for debugging if available
            if (rawLlmResponse != null) {
                try {
                    writer.writeDebugOutput(rawLlmResponse);
                } catch (Exception writeEx) {
                    System.err.println("Could not write debug output: " + writeEx.getMessage());
                }
            }
            System.exit(1);
        }
    }

    /**
     * Loads the OpenAI API key from:
     * 1. .env file in project root (preferred for local dev)
     * 2. System environment variable (preferred for CI/CD)
     */
    private static String loadApiKey() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()   // Don't throw if .env doesn't exist
                    .load();
            String key = dotenv.get("OPENAI_API_KEY");
            if (key != null && !key.isBlank()) {
                System.out.println("AUTH: API Key loaded from .env file.");
                return key;
            }
        } catch (Exception e) {
            // Fall through to environment variable check
        }

        // Try system environment variable
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            System.out.println("AUTH: API Key loaded from environment variable.");
            return envKey;
        }

        return null;
    }

    /**
     * Prints the final console summary after successful generation.
     */
    private static void printConsoleSummary(TestCaseResponse response,
                                             String requirementFile,
                                             OutputWriter writer) {
        int total = response.getTestScenarios() != null ? response.getTestScenarios().size() : 0;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("   SUCCESS: TEST CASE GENERATION COMPLETE");
        System.out.println("=".repeat(60));
        System.out.printf("  Requirement File      : %s%n", requirementFile);
        System.out.printf("  Feature Name          : %s%n", response.getFeatureName());
        System.out.printf("  Validation Status     : PASSED %n");
        System.out.println("-".repeat(60));
        System.out.printf("  Total Scenarios       : %d%n", total);
        System.out.printf("  High Priority         : %d%n", response.countByPriority("High"));
        System.out.printf("  Medium Priority       : %d%n", response.countByPriority("Medium"));
        System.out.printf("  Low Priority          : %d%n", response.countByPriority("Low"));
        System.out.printf("  Negative Scenarios    : %d%n", response.countNegativeScenarios());
        System.out.printf("  Automation Candidates : %d%n", response.countAutomationCandidates());
        System.out.println("-".repeat(60));
        System.out.printf("  Output JSON           : %s%n", writer.getJsonOutputPath());
        System.out.printf("  Summary Report        : %s%n", writer.getSummaryPath());
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Prints the startup banner.
     */
    private static void printBanner() {
        System.out.println("""
                
                ╔══════════════════════════════════════════════════════════╗
                ║       AI TEST CASE GENERATOR - Controlled LLM Output     ║
                ║       Java + OpenAI + JSON Schema Validation             ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }
}
