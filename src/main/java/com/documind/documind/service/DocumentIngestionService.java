package com.documind.documind.service;

import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DocumentIngestionService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    public DocumentIngestionService(ChunkingService chunkingService, EmbeddingService embeddingService) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    public void ingest(String filePath) throws Exception {
        List<TextSegment> chunks = chunkingService.chunkDocument(filePath);
        embeddingService.embedAndStore(chunks);
    }
}
