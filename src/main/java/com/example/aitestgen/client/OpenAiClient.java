package com.example.aitestgen.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Module 3: OpenAI API Client
 *
 * Handles all HTTP communication with the OpenAI Chat Completions API.
 *
 * Features:
 * - Uses OkHttp for robust HTTP calls
 * - Builds the request payload (system + user messages)
 * - Extracts the raw JSON response content from the API response
 * - Strips markdown code blocks if the LLM adds them despite instructions
 * - Throws meaningful exceptions on API errors
 */
public class OpenAiClient {

    // OpenAI Chat Completions endpoint
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // GPT-4o model for best instruction-following and JSON output
    private static final String MODEL = "gpt-4o";

    // Media type for JSON request body
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructor: Initializes OkHttp client with timeouts.
     *
     * @param apiKey Your OpenAI API key (loaded from .env or environment variable).
     */
    public OpenAiClient(String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();

        // Configure OkHttp with generous timeouts for LLM calls
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)   // LLM responses can take time
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sends a chat completion request to OpenAI with system and user messages.
     *
     * @param systemPrompt The system message that sets LLM behavior/persona.
     * @param userPrompt   The user message containing the requirement and schema.
     * @return Raw string response from the LLM (should be JSON).
     * @throws IOException If the HTTP call fails or returns an error.
     */
    public String callChatCompletion(String systemPrompt, String userPrompt) throws IOException {
        System.out.println("[OpenAiClient] Calling OpenAI API with model: " + MODEL);

        // Build JSON request body
        String requestBody = buildRequestBody(systemPrompt, userPrompt);

        // Build HTTP request with Authorization header
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                .build();

        // Execute the HTTP call
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("[OpenAiClient] API call failed. HTTP " + response.code()
                        + " | Error: " + errorBody);
            }

            String responseBody = response.body().string();
            System.out.println("[OpenAiClient] API call successful. Extracting content...");

            return extractContent(responseBody);
        }
    }

    /**
     * Builds the JSON request body for the Chat Completions API.
     *
     * Format:
     * {
     *   "model": "gpt-4o",
     *   "temperature": 0.2,
     *   "messages": [
     *     {"role": "system", "content": "..."},
     *     {"role": "user", "content": "..."}
     *   ]
     * }
     *
     * Low temperature (0.2) = more deterministic, less creative output.
     * This is important for structured JSON generation.
     */
    private String buildRequestBody(String systemPrompt, String userPrompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", MODEL);
        root.put("temperature", 0.2);   // Low temperature for structured output
        root.put("max_tokens", 4096);   // Allow enough room for full response

        // Build messages array
        ArrayNode messages = objectMapper.createArrayNode();

        // System message
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // User message
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        root.set("messages", messages);

        // Enable JSON mode for better structured output (GPT-4o supports this)
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");
        root.set("response_format", responseFormat);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Extracts the actual text content from the OpenAI response JSON.
     *
     * The OpenAI API response structure:
     * {
     *   "choices": [
     *     {
     *       "message": {
     *         "content": "...actual response here..."
     *       }
     *     }
     *   ]
     * }
     *
     * Also strips markdown code fences if the LLM adds them despite instructions.
     */
    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");

        if (choices.isEmpty()) {
            throw new IOException("[OpenAiClient] No 'choices' found in API response.");
        }

        String content = choices.get(0).path("message").path("content").asText();

        if (content == null || content.isBlank()) {
            throw new IOException("[OpenAiClient] Empty content in API response.");
        }

        // Strip markdown code fences if LLM ignores the no-markdown instruction
        content = stripMarkdownFences(content.trim());

        System.out.println("[OpenAiClient] Content extracted. Length: " + content.length() + " chars.");
        return content;
    }

    /**
     * Removes markdown code block fences (```json ... ``` or ``` ... ```) from LLM output.
     * This is a safety net for when the LLM adds them despite instructions not to.
     *
     * @param content Raw LLM content.
     * @return Cleaned content without markdown fences.
     */
    private String stripMarkdownFences(String content) {
        // Remove ```json ... ``` or ``` ... ``` wrappers
        if (content.startsWith("```")) {
            // Find the first newline after the opening fence
            int firstNewline = content.indexOf('\n');
            if (firstNewline != -1) {
                content = content.substring(firstNewline + 1);
            }
            // Remove the closing fence
            if (content.endsWith("```")) {
                content = content.substring(0, content.lastIndexOf("```")).trim();
            }
        }
        return content.trim();
    }
}
