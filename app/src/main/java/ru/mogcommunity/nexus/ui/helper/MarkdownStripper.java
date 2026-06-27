package ru.mogcommunity.rbrproject.ui.helper;

public class MarkdownStripper {
    public static String strip(String text) {
        if (text == null) return "";

        text = text.replaceAll("```", "");
        text = text.replaceAll("`", "");

        text = text.replaceAll("(?m)^#+\\s+", "");

        text = text.replaceAll("\\*\\*\\*", "");
        text = text.replaceAll("\\*\\*", "");
        text = text.replaceAll("\\*", "");
        text = text.replaceAll("___", "");
        text = text.replaceAll("__", "");
        text = text.replaceAll("_", "");

        text = text.replaceAll("(?m)^[-+*]\\s+", "• ");

        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "$1 ($2)");

        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].trim());
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }
}
