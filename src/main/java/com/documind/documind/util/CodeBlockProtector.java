package com.documind.documind.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeBlockProtector {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");

    public static class ProtectedText {
        public final String textWithPlaceholders;
        public final Map<String, String> codeBlockMap;

        public ProtectedText(String textWithPlaceholders, Map<String, String> codeBlockMap) {
            this.textWithPlaceholders = textWithPlaceholders;
            this.codeBlockMap = codeBlockMap;
        }
    }


    public static ProtectedText protect(String text) {
        Map<String, String> codeBlockMap = new HashMap<>();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);

        StringBuilder protectedText = new StringBuilder();
        int lastEnd = 0;
        int codeIndex = 0;

        while (matcher.find()) {
            protectedText.append(text, lastEnd, matcher.start());
            String placeholder = "___CODE_BLOCK_" + codeIndex + "___";
            codeBlockMap.put(placeholder, matcher.group());
            protectedText.append(placeholder);
            codeIndex++;
            lastEnd = matcher.end();
        }
        protectedText.append(text.substring(lastEnd));

        return new ProtectedText(protectedText.toString(), codeBlockMap);
    }

    public static String restore(String chunkText, Map<String, String> codeBlockMap) {
        String restored = chunkText;
        for (Map.Entry<String, String> entry : codeBlockMap.entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    public static boolean containsCode(String text) {
        return text.contains("```");
    }
}
