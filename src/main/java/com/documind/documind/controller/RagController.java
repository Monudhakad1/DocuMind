package com.documind.documind.controller;


import com.documind.documind.service.ChunkingService;
import com.documind.documind.service.DocumentIngestionService;
import com.documind.documind.service.EmbeddingService;
import com.documind.documind.service.RagQueryService;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final RagQueryService ragQueryService;

    public RagController(DocumentIngestionService ingestionService,
                          ChunkingService chunkingService,
                          EmbeddingService embeddingService,
                          RagQueryService ragQueryService) {
        this.ingestionService = ingestionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.ragQueryService = ragQueryService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        File tempFile = File.createTempFile("upload-", ".pdf");
        file.transferTo(tempFile);

        List<TextSegment> chunks = chunkingService.chunkDocument(tempFile.getAbsolutePath());
        embeddingService.embedAndStore(chunks);
        ragQueryService.buildPageIndex(chunks);

        Files.deleteIfExists(tempFile.toPath());

        return ResponseEntity.ok("Ingested successfully. Total chunks: " + chunks.size());
    }


    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String question) {
        String answer = ragQueryService.ask(question);
        return ResponseEntity.ok(answer);
    }
}
