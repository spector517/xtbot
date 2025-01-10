package com.github.spector517.xtbot.lib.executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextExecutorsTest {

    @Test
    @DisplayName("Test match: Value matches by pattern")
    void testMatch_0() {
        var patterns = Arrays.asList(".*a.*", ".*b.*");
        var val = "abc";
        var result = TextExecutors.match(patterns, val);
        assertTrue(result);
    }

    @Test
    @DisplayName("Test match: Value does not match by pattern")
    void testMatch_1() {
        var patterns = Arrays.asList(".*a.*", ".*b.*");
        var val = "xyz";
        var result = TextExecutors.match(patterns, val);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test match: Value is null, patters is not blank")
    void testMatch_2() {
        var patterns = Arrays.asList(".*a.*", ".*b.*");
        var result = TextExecutors.match(patterns, null);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test match: Value is null, patters is blank")
    void testMatch_3() {
        assertTrue(TextExecutors.match(List.of(), null));
        assertTrue(TextExecutors.match(null, null));
    }

    @Test
    @DisplayName("Test match: Pattern is null")
    void testMatch_4() {
        var val = "abc";
        var result = TextExecutors.match(null, val);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test split: Value is null")
    void testSplit_0() {
        var val = "abc,def,ghi";
        var delimiter = ",";
        var result = TextExecutors.split(val, delimiter);
        assertEquals(Arrays.asList("abc", "def", "ghi"), result);
    }

    @Test
    @DisplayName("Test split: Value is empty")
    void testSplit_1() {
        var val = "abc,def,ghi";
        var delimiter = "";
        var result = TextExecutors.split(val, delimiter);
        assertEquals(Arrays.asList("a", "b", "c", ",", "d", "e", "f", ",", "g", "h", "i"), result);
    }

    @Test
    @DisplayName("Test split: Val is null")
    void testSplit_2() {
        var delimiter = ",";
        var result = TextExecutors.split(null, delimiter);
        assertEquals(List.of(), result);
    }

    @Test
    @DisplayName("Test split: Delimiter is null")
    void testSplit_3() {
        var val = "abc,def,ghi";
        var result = TextExecutors.split(val, null);
        assertEquals(List.of(val), result);
    }
}
