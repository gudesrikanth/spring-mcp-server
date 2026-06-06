package com.portfolio.mcp.toolbox;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stateless "developer toolbox" utilities exposed as MCP tools.
 *
 * <p>These tools take no external dependencies, which makes them ideal for
 * learning how an MCP client discovers ({@code tools/list}) and invokes
 * ({@code tools/call}) tools. Each public method annotated with
 * {@link McpTool} becomes a callable tool; the {@code name} is what clients
 * (and LLMs) reference, and parameter descriptions feed the generated JSON
 * schema the client sees.
 */
@Component
public class ToolboxTools {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @McpTool(name = "gen_uuid", description = "Generate a random RFC-4122 version 4 UUID.")
    public String genUuid() {
        return UUID.randomUUID().toString();
    }

    @McpTool(name = "base64_encode", description = "Base64-encode a UTF-8 string.")
    public String base64Encode(
            @McpToolParam(description = "The plain text to encode") String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @McpTool(name = "base64_decode", description = "Decode a Base64 string back to UTF-8 text.")
    public String base64Decode(
            @McpToolParam(description = "The Base64 text to decode") String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    @McpTool(name = "hash", description = "Compute a cryptographic hash of some text. "
            + "Supported algorithms: MD5, SHA-1, SHA-256, SHA-512.")
    public String hash(
            @McpToolParam(description = "The text to hash") String text,
            @McpToolParam(description = "Algorithm: MD5, SHA-1, SHA-256 or SHA-512") String algorithm) {
        String algo = (algorithm == null || algorithm.isBlank()) ? "SHA-256" : algorithm.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            byte[] out = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algo
                    + ". Try one of MD5, SHA-1, SHA-256, SHA-512.");
        }
    }

    @McpTool(name = "json_pretty", description = "Pretty-print (indent) a compact JSON string.")
    public String jsonPretty(
            @McpToolParam(description = "A valid JSON document as a string") String json) {
        try {
            Object tree = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input is not valid JSON: " + e.getMessage());
        }
    }

    @McpTool(name = "regex_test", description = "Test whether a regular expression matches an input "
            + "string. Returns whether it matched and any captured groups.")
    public RegexResult regexTest(
            @McpToolParam(description = "The regular expression pattern") String pattern,
            @McpToolParam(description = "The input string to test against") String input) {
        try {
            var matcher = Pattern.compile(pattern).matcher(input);
            boolean found = matcher.find();
            String[] groups = new String[found ? matcher.groupCount() + 1 : 0];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = matcher.group(i);
            }
            return new RegexResult(found, groups);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
        }
    }

    @McpTool(name = "to_epoch", description = "Convert an ISO-8601 timestamp "
            + "(e.g. 2026-06-06T10:15:30Z) to Unix epoch seconds.")
    public long toEpoch(
            @McpToolParam(description = "ISO-8601 instant, e.g. 2026-06-06T10:15:30Z") String iso) {
        return Instant.parse(iso).getEpochSecond();
    }

    @McpTool(name = "from_epoch", description = "Convert Unix epoch seconds to an ISO-8601 UTC timestamp.")
    public String fromEpoch(
            @McpToolParam(description = "Unix time in seconds since 1970-01-01T00:00:00Z") long seconds) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(seconds));
    }

    /** Structured result for {@link #regexTest}. Serialized to JSON for the client. */
    public record RegexResult(boolean matched, String[] groups) {
    }
}
