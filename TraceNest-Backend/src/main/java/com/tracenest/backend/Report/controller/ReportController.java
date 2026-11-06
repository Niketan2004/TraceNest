package com.tracenest.backend.Report.controller;

import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Common.dto.PaginatedResponse;
import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Report.dto.CreateReportRequest;
import com.tracenest.backend.Report.dto.ReportDto;
import com.tracenest.backend.Report.dto.UpdateReportStatusRequest;
import com.tracenest.backend.Report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for emergency report creation, search, geospatial filtering,
 * owner management, status updates, and dynamic A4 PDF poster downloads.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Searches and retrieves a paginated collection of public emergency reports.
     *
     * @param q Free-text search term.
     * @param type Category filter (person, pet, lost-vehicle, stolen-vehicle).
     * @param status Status filter (missing, found, resolved).
     * @param city Target city string.
     * @param lat Center latitude coordinate.
     * @param lng Center longitude coordinate.
     * @param radiusKm Radius boundary in kilometers.
     * @param sort Sort order ('newest' or 'reward').
     * @param page Zero-indexed page number.
     * @param size Page size.
     * @return Paginated response wrapping {@link ReportDto} items.
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ReportDto>> getReports(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reportService.getReports(q, type, status, city, lat, lng, radiusKm, sort, page, size));
    }

    /**
     * Retrieves full case dossier for a specific report ID.
     *
     * @param id Report unique identifier.
     * @return Detailed {@link ReportDto}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> getReportById(@PathVariable String id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    /**
     * Creates a new emergency report with up to 5 uploaded image files.
     *
     * @param userDetails Authenticated reporter security principal.
     * @param request Validated JSON report details payload.
     * @param images Multipart image attachments.
     * @return Created {@link ReportDto} with HTTP 201 Created.
     */
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportDto> createReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestPart("request") @Valid CreateReportRequest request,
            @RequestPart(value = "images", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> images) {
        
        if (images == null || images.isEmpty() || images.size() > 5) {
            throw new CustomException(400, "Bad Request", "BAD_REQUEST", "Please provide between 1 and 5 images");
        }
        for (org.springframework.web.multipart.MultipartFile file : images) {
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new CustomException(400, "Bad Request", "BAD_REQUEST", "Image " + file.getOriginalFilename() + " exceeds 5MB");
            }
        }

        ReportDto report = reportService.createReport(userDetails.getId(), request, images);
        return new ResponseEntity<>(report, HttpStatus.CREATED);
    }

    /**
     * Modifies report details, enforcing reporter ownership.
     *
     * @param userDetails Authenticated user principal.
     * @param id Target report identifier.
     * @param request Updated report values.
     * @return Updated {@link ReportDto}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ReportDto> updateReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id,
            @RequestBody CreateReportRequest request) {
        return ResponseEntity.ok(reportService.updateReport(userDetails.getId(), id, request));
    }

    /**
     * Updates the status of an authored report (MISSING, FOUND, RESOLVED).
     *
     * @param userDetails Authenticated user principal.
     * @param id Report identifier.
     * @param request Validated status transition payload.
     * @return Updated {@link ReportDto}.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ReportDto> updateReportStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        return ResponseEntity.ok(reportService.updateReportStatus(userDetails.getId(), id, request));
    }

    /**
     * Permanently deletes a report and removes all related media assets and notifications.
     *
     * @param userDetails Authenticated user principal.
     * @param id Report identifier.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        reportService.deleteReport(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all reports authored by the current authenticated user.
     *
     * @param userDetails Authenticated user principal.
     * @param page Zero-indexed page number.
     * @param size Page slice size.
     * @return Paginated collection of author's reports.
     */
    @GetMapping("/mine")
    public ResponseEntity<PaginatedResponse<ReportDto>> getMyReports(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getMyReports(userDetails.getId(), page, size));
    }

    /**
     * Computes platform-wide report metrics and aggregates.
     *
     * @return Map containing total, active, found, resolved, and category metrics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(reportService.getStats());
    }

    /**
     * Retrieves geocoded reports for plotting on interactive Leaflet maps.
     *
     * @param q Query search term.
     * @param type Category filter.
     * @param status Status filter.
     * @param city Target city.
     * @param lat Center latitude.
     * @param lng Center longitude.
     * @param radiusKm Radius boundary in km.
     * @return Paginated map data points.
     */
    @GetMapping("/map")
    public ResponseEntity<PaginatedResponse<ReportDto>> getMapReports(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm
    ) {
        return ResponseEntity.ok(reportService.getMapReports(q, type, status, city, lat, lng, radiusKm));
    }

    /**
     * Generates and returns a downloadable high-resolution A4 PDF emergency flyer.
     *
     * @param id Target report identifier.
     * @return {@link ResponseEntity} streaming PDF bytes with inline Content-Disposition header.
     */
    @GetMapping(value = "/{id}/poster", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReportPoster(@PathVariable String id) {
        byte[] pdf = reportService.getReportPoster(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tracenest-poster-" + id + ".pdf\"")
                .body(pdf);
    }
}
