package com.documind.documind.service;

import com.documind.documind.util.CodeBlockProtector;
import com.documind.documind.util.PdfTextExtractor;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 1500;
    private static final int CHUNK_OVERLAP = 200;

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "(?=\\n\\d+(\\.\\d+)*\\.?\\s+[A-Z][^\\n]{2,80}\\n)"
    );

    public List<TextSegment> chunkDocument(String filePath) throws Exception {
        List<TextSegment> allSegments = new ArrayList<>();

        Map<Integer, String> pageTextMap = PdfTextExtractor.extractPageWiseText(filePath);

        for (Map.Entry<Integer, String> entry : pageTextMap.entrySet()) {
            int pageNum = entry.getKey();
            String pageText = entry.getValue();

            List<TextSegment> pageSegments = processPageText(pageText, pageNum, filePath);
            allSegments.addAll(pageSegments);
        }

        System.out.println("Total chunks created: " + allSegments.size());
        return allSegments;
    }

    private List<TextSegment> processPageText(String pageText, int pageNum, String sourceFile) {
        List<TextSegment> segments = new ArrayList<>();

        List<String> sections = splitByHeadings(pageText);

        for (String sectionText : sections) {
            if (sectionText.trim().isEmpty()) continue;

            String heading = extractHeading(sectionText);
            List<String> safeChunks = splitWithCodeProtection(sectionText.trim());

            for (String chunkText : safeChunks) {
                TextSegment segment = TextSegment.from(chunkText);
                segment.metadata().put("page_number", String.valueOf(pageNum));
                segment.metadata().put("section", heading);
                segment.metadata().put("source", sourceFile);
                segment.metadata().put("contains_code", String.valueOf(CodeBlockProtector.containsCode(chunkText)));
                segments.add(segment);
            }
        }

        return segments;
    }


    private List<String> splitByHeadings(String text) {
        Matcher matcher = HEADING_PATTERN.matcher(text);
        List<Integer> splitPoints = new ArrayList<>();
        splitPoints.add(0);
        while (matcher.find()) {
            splitPoints.add(matcher.start());
        }
        splitPoints.add(text.length());

        List<String> sections = new ArrayList<>();
        for (int j = 0; j < splitPoints.size() - 1; j++) {
            int start = splitPoints.get(j);
            int end = splitPoints.get(j + 1);
            if (end > start) {
                sections.add(text.substring(start, end));
            }
        }
        return sections.isEmpty() ? List.of(text) : sections;
    }

    private String extractHeading(String sectionText) {
        String[] lines = sectionText.trim().split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                return line.trim().length() > 60 ? line.trim().substring(0, 60) : line.trim();
            }
        }
        return "Unknown Section";
    }


    private List<String> splitWithCodeProtection(String sectionText) {
        if (sectionText.length() <= CHUNK_SIZE) {
            return List.of(sectionText);
        }

        CodeBlockProtector.ProtectedText protectedText = CodeBlockProtector.protect(sectionText);

        DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        List<TextSegment> rawChunks = splitter.split(Document.from(protectedText.textWithPlaceholders));

        List<String> finalChunks = new ArrayList<>();
        for (TextSegment chunk : rawChunks) {
            String restored = CodeBlockProtector.restore(chunk.text(), protectedText.codeBlockMap);
            finalChunks.add(restored);
        }

        return finalChunks;
    }
}
