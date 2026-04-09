package com.example.aitestgen.reader;

import com.example.aitestgen.util.FileUtils;

/**
 * Module 1: Requirement Reader
 *
 * Reads requirement text from:
 * - A .txt file on disk
 * - A direct raw string passed by the caller
 *
 * Also normalizes the text (trims whitespace, removes extra blank lines, etc.)
 * before it is sent to the PromptBuilder.
 */
public class RequirementReader {

    /**
     * Reads requirement text from a file and normalizes it.
     *
     * @param filePath Path to the requirement .txt file.
     * @return Normalized requirement text.
     * @throws Exception If the file cannot be read.
     */
    public String readFromFile(String filePath) throws Exception {
        System.out.println("[RequirementReader] Reading requirement from file: " + filePath);
        String raw = FileUtils.readFile(filePath);
        return normalize(raw);
    }

    /**
     * Accepts a raw requirement string (for programmatic use or testing).
     *
     * @param rawRequirement The raw requirement text.
     * @return Normalized requirement text.
     */
    public String readFromString(String rawRequirement) {
        System.out.println("[RequirementReader] Reading requirement from direct string input.");
        return normalize(rawRequirement);
    }

    /**
     * Normalizes requirement text:
     * - Trims leading/trailing whitespace
     * - Removes Windows-style carriage returns (\r)
     * - Collapses multiple blank lines into a single blank line
     * - Ensures the text is non-empty
     *
     * @param raw The raw input text.
     * @return Cleaned, normalized text.
     * @throws IllegalArgumentException If the requirement is empty after normalization.
     */
    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("[RequirementReader] Requirement text is empty or null.");
        }

        // Remove carriage returns (Windows line endings)
        String normalized = raw.replace("\r", "");

        // Trim leading/trailing whitespace
        normalized = normalized.trim();

        // Collapse multiple consecutive blank lines into one
        normalized = normalized.replaceAll("(?m)^\\s*$\\n{2,}", "\n\n");

        System.out.println("[RequirementReader] Requirement normalized. Length: " + normalized.length() + " chars.");
        return normalized;
    }
}
