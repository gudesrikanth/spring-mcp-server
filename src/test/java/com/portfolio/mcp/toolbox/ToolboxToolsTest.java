package com.portfolio.mcp.toolbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests for the stateless toolbox tools — no Spring context needed.
 */
class ToolboxToolsTest {

    private final ToolboxTools tools = new ToolboxTools();

    @Test
    void genUuid_isParseableUuid() {
        String uuid = tools.genUuid();
        assertThat(java.util.UUID.fromString(uuid)).hasToString(uuid);
    }

    @Test
    void base64_roundTrips() {
        String encoded = tools.base64Encode("hello world");
        assertThat(encoded).isEqualTo("aGVsbG8gd29ybGQ=");
        assertThat(tools.base64Decode(encoded)).isEqualTo("hello world");
    }

    @Test
    void hash_sha256_matchesKnownValue() {
        // Well-known SHA-256 of the empty string.
        assertThat(tools.hash("", "SHA-256"))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void hash_unknownAlgorithm_throws() {
        assertThatThrownBy(() -> tools.hash("x", "NOPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported algorithm");
    }

    @Test
    void jsonPretty_indentsValidJson() {
        String pretty = tools.jsonPretty("{\"a\":1,\"b\":2}");
        assertThat(pretty).contains("\n").contains("\"a\"").contains("\"b\"");
    }

    @Test
    void jsonPretty_invalidJson_throws() {
        assertThatThrownBy(() -> tools.jsonPretty("{not json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void regexTest_capturesGroups() {
        var result = tools.regexTest("(\\d+)-(\\d+)", "order 42-99 shipped");
        assertThat(result.matched()).isTrue();
        assertThat(result.groups()).containsExactly("42-99", "42", "99");
    }

    @Test
    void regexTest_noMatch() {
        var result = tools.regexTest("\\d+", "no digits here");
        assertThat(result.matched()).isFalse();
        assertThat(result.groups()).isEmpty();
    }

    @Test
    void epoch_roundTrips() {
        long epoch = tools.toEpoch("2026-06-06T00:00:00Z");
        assertThat(epoch).isEqualTo(1780704000L);
        assertThat(tools.fromEpoch(epoch)).isEqualTo("2026-06-06T00:00:00Z");
    }
}
