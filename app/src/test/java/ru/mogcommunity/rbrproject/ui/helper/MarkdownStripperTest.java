package ru.mogcommunity.rbrproject.ui.helper;

import static org.junit.Assert.*;

import org.junit.Test;

public class MarkdownStripperTest {

    @Test
    public void strip_removesHeaders() {
        String input = "# Header\n## Subheader\n### Third";
        String result = MarkdownStripper.strip(input);

        assertFalse(result.contains("#"));
        assertTrue(result.contains("Header"));
        assertTrue(result.contains("Subheader"));
    }

    @Test
    public void strip_removesBoldAndItalic() {
        String input = "**bold** and *italic* and __underline__ and ___both___";
        String result = MarkdownStripper.strip(input);

        assertFalse(result.contains("*"));
        assertFalse(result.contains("_"));
        assertTrue(result.contains("bold"));
        assertTrue(result.contains("italic"));
    }

    @Test
    public void strip_removesCodeBlocks() {
        String input = "```java\ncode here\n```";
        String result = MarkdownStripper.strip(input);

        assertFalse(result.contains("```"));
        assertTrue(result.contains("code here"));
    }

    @Test
    public void strip_removesInlineCode() {
        String input = "use `println` method";
        String result = MarkdownStripper.strip(input);

        assertFalse(result.contains("`"));
        assertTrue(result.contains("println"));
    }

    @Test
    public void strip_convertsLinks() {
        String input = "[Google](https://google.com)";
        String result = MarkdownStripper.strip(input);

        assertTrue(result.contains("Google"));
        assertTrue(result.contains("https://google.com"));
        assertFalse(result.contains("["));
        assertFalse(result.contains("]"));
    }

    @Test
    public void strip_convertsBulletPoints() {
        String input = "- item one\n- item two";
        String result = MarkdownStripper.strip(input);

        assertFalse(result.startsWith("-"));
    }

    @Test
    public void strip_handlesNull() {
        String result = MarkdownStripper.strip(null);
        assertEquals("", result);
    }

    @Test
    public void strip_handlesEmptyString() {
        String result = MarkdownStripper.strip("");
        assertEquals("", result);
    }

    @Test
    public void strip_preservesPlainText() {
        String input = "This is plain text without any formatting";
        String result = MarkdownStripper.strip(input);

        assertEquals("This is plain text without any formatting", result);
    }

    @Test
    public void strip_trimsResult() {
        String input = "  some text  ";
        String result = MarkdownStripper.strip(input);

        assertEquals("some text", result);
    }
}
