package com.example.aitestgen.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Utility class for common file I/O operations.
 *
 * Provides:
 * - Reading files as strings
 * - Writing strings to files
 * - Ensuring directories exist
 * - Checking file existence
 */
public class FileUtils {

    // Private constructor to prevent instantiation — this is a utility class
    private FileUtils() {}

    /**
     * Reads the entire content of a file and returns it as a string.
     *
     * @param filePath Absolute or relative path to the file.
     * @return File content as a String.
     * @throws IOException If the file cannot be found or read.
     */
    public static String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Writes text content to a file, creating the file and parent directories if needed.
     * If the file already exists, it will be overwritten.
     *
     * @param filePath Path to write the file.
     * @param content  Text content to write.
     * @throws IOException If the file cannot be written.
     */
    public static void writeFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        // Create parent directories if they do not exist
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Creates a directory (and all parent directories) if it does not already exist.
     *
     * @param dirPath Path to the directory.
     * @throws IOException If the directory cannot be created.
     */
    public static void ensureDirectoryExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Checks whether a file exists at the given path.
     *
     * @param filePath Path to check.
     * @return true if file exists, false otherwise.
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Reads a file from the classpath (resources folder).
     * Useful for reading schema files bundled inside the JAR.
     *
     * @param resourcePath The path relative to the classpath root.
     * @return Content of the resource as a String.
     * @throws IOException If the resource cannot be found or read.
     */
    public static String readResourceFile(String resourcePath) throws IOException {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Returns the file name from a given path (without directories).
     *
     * @param filePath Full file path.
     * @return File name only.
     */
    public static String getFileName(String filePath) {
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }
}
