package com.tracenest.backend.Report.service;

import com.tracenest.backend.Report.model.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service hook prepared for future AI processing pipeline (Next Semester).
 * Conceptually handles:
 * - Vision embedding extraction (CLIP / DINOv2)
 * - Text embedding & semantic indexing (SBERT)
 * - OCR extraction for vehicle plates / posters
 * - Multimodal vector matching
 *
 * Current status: Clean architecture stub ready for integration without fake results.
 */
@Service
@Slf4j
public class AiProcessingService {

    @Async
    public void processNewReport(Report report) {
        log.info("[AI-Pipeline-Hook] Report {} enqueued for future AI multimodal pipeline (deferred to next semester)", report.getId());
        // Future implementation:
        // 1. extractVisionEmbeddings(report.getImages());
        // 2. extractOcrText(report.getImages());
        // 3. computeTextEmbedding(report.getTitle(), report.getDescription());
        // 4. triggerVectorSearchMatches(report);
    }
}
