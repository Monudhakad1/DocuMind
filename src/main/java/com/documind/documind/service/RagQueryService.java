package com.documind.documind.service;

import com.documind.documind.config.ChatModelConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RagQueryService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;

    // page_number -> us page ke saare TextSegments (neighbor lookup ke liye)
    private final Map<Integer, List<TextSegment>> segmentsByPage = new HashMap<>();

    public RagQueryService(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ChatModel chatModel) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
    }


    public void buildPageIndex(List<TextSegment> allSegments) {
        segmentsByPage.clear();
        for (TextSegment segment : allSegments) {
            String pageStr = segment.metadata().getString("page_number");
            if (pageStr == null) continue;
            int page = Integer.parseInt(pageStr);
            segmentsByPage.computeIfAbsent(page, k -> new ArrayList<>()).add(segment);
        }
    }

    public String ask(String userQuestion) {
        String context = retrieveWithNeighborContext(userQuestion, 3);

        String prompt = """
You are an expert software engineer.

Use the documentation below as the primary source.

If the documentation is outdated or uses an older implementation, provide the latest recommended implementation and explain the difference.

If the documentation does not fully answer the question, complete the answer using your software engineering knowledge.

Documentation:
%s

Question:
%s
""".formatted(context,userQuestion);

        return chatModel.chat(prompt);
    }

    private String retrieveWithNeighborContext(String userQuery, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        Set<Integer> relevantPages = new TreeSet<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            String pageStr = match.embedded().metadata().getString("page_number");
            if (pageStr == null) continue;
            int page = Integer.parseInt(pageStr);
            relevantPages.add(page - 1); // previous page
            relevantPages.add(page);     // matched page
            relevantPages.add(page + 1); // next page
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (Integer page : relevantPages) {
            List<TextSegment> pageSegments = segmentsByPage.get(page);
            if (pageSegments == null) continue;

            contextBuilder.append("\n--- Page ").append(page).append(" ---\n");
            for (TextSegment seg : pageSegments) {
                contextBuilder.append(seg.text()).append("\n");
            }
        }

        return contextBuilder.toString();
    }
}
