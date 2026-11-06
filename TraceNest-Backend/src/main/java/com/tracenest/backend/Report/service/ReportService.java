package com.tracenest.backend.Report.service;

import com.tracenest.backend.Common.dto.PaginatedResponse;
import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Notification.service.NotificationService;
import com.tracenest.backend.Report.dto.CreateReportRequest;
import com.tracenest.backend.Report.dto.ReportDto;
import com.tracenest.backend.Report.dto.UpdateReportStatusRequest;
import com.tracenest.backend.Report.mapper.ReportMapper;
import com.tracenest.backend.Report.model.Report;
import com.tracenest.backend.Report.model.ReportStatus;
import com.tracenest.backend.Report.model.ReportType;
import com.tracenest.backend.Report.repository.ReportRepository;
import com.tracenest.backend.Subscription.service.SubscriptionService;
import com.tracenest.backend.Upload.service.CloudinaryService;
import com.tracenest.backend.User.model.User;
import com.tracenest.backend.User.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business service orchestrating incident report lifecycles, dynamic geospatial filtering,
 * media file handling, OpenPDF poster triggers, and subscriber notification fan-out.
 */
@Service
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ReportMapper reportMapper;
    private final CloudinaryService cloudinaryService;
    private final SubscriptionService subscriptionService;
    private final PosterService posterService;
    private final AiProcessingService aiProcessingService;

    /**
     * Constructs the report service with necessary domain collaborators.
     */
    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            ReportMapper reportMapper,
            CloudinaryService cloudinaryService,
            SubscriptionService subscriptionService,
            PosterService posterService,
            AiProcessingService aiProcessingService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.reportMapper = reportMapper;
        this.cloudinaryService = cloudinaryService;
        this.subscriptionService = subscriptionService;
        this.posterService = posterService;
        this.aiProcessingService = aiProcessingService;
    }

    /**
     * Queries reports using dynamic JPA specifications with keyword matching,
     * category filtering, status conditions, and geospatial bounding-box queries.
     *
     * @param q Free-text search term queried across title and description.
     * @param type Report category enum filter (person, pet, lost-vehicle, stolen-vehicle).
     * @param status Incident status filter (missing, found, resolved).
     * @param city City name filter.
     * @param lat Center latitude for distance calculations.
     * @param lng Center longitude for distance calculations.
     * @param radiusKm Radius boundary in kilometers.
     * @param sortStr Ordering criteria ('newest' or 'reward').
     * @param page Zero-indexed page number.
     * @param size Page slice size.
     * @return {@link PaginatedResponse} containing matching {@link ReportDto} items.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ReportDto> getReports(String q, String type, String status, String city,
                                                   Double lat, Double lng, Double radiusKm,
                                                   String sortStr, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("reward".equalsIgnoreCase(sortStr)) {
            sort = Sort.by(Sort.Direction.DESC, "reward").and(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Report> spec = Specification.where(null);
        
        if (StringUtils.hasText(q)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + q.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + q.toLowerCase() + "%")
            ));
        }
        if (StringUtils.hasText(type) && !"all".equalsIgnoreCase(type)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), ReportType.valueOf(type.toUpperCase().replace("-", "_"))));
        }
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ReportStatus.valueOf(status.toUpperCase())));
        }
        if (StringUtils.hasText(city)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location").get("city")), "%" + city.toLowerCase() + "%"));
        }
        
        // Geo-bounding box filtering
        if (lat != null && lng != null && radiusKm != null && radiusKm > 0) {
            double latDelta = radiusKm / 111.0;
            double lngDelta = radiusKm / (111.0 * Math.max(0.1, Math.cos(Math.toRadians(lat))));
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.between(root.get("location").get("lat"), lat - latDelta, lat + latDelta),
                    cb.between(root.get("location").get("lng"), lng - lngDelta, lng + lngDelta)
            ));
        }

        Page<Report> reportPage = reportRepository.findAll(spec, pageable);
        Page<ReportDto> dtoPage = reportPage.map(reportMapper::reportToReportDto);

        return PaginatedResponse.of(dtoPage);
    }

    /**
     * Resolves single report entity by its unique ID.
     *
     * @param id The report identifier.
     * @return Resolved {@link ReportDto}.
     * @throws CustomException with HTTP 404 if the report does not exist.
     */
    @Transactional(readOnly = true)
    public ReportDto getReportById(String id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Report not found"));
        return reportMapper.reportToReportDto(report);
    }

    /**
     * Renders a printable high-resolution A4 emergency flyer / poster as a PDF byte stream.
     *
     * @param id The report identifier.
     * @return Generated PDF document as raw byte array.
     */
    @Transactional(readOnly = true)
    public byte[] getReportPoster(String id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Report not found"));
        return posterService.generatePoster(report);
    }

    /**
     * Creates a new report record, uploads attached images to Cloudinary,
     * persists the entity, and dispatches proximity alert notifications.
     *
     * @param userId The ID of the authenticated user creating the report.
     * @param request Validated report payload containing attributes and location.
     * @param images List of 1 to 5 uploaded multipart image files.
     * @return The persisted {@link ReportDto}.
     */
    @Transactional
    public ReportDto createReport(String userId, CreateReportRequest request, java.util.List<org.springframework.web.multipart.MultipartFile> images) {
        User reporter = userRepository.findById(userId).orElseThrow();

        List<String> imageUrls = new java.util.ArrayList<>();
        try {
            if (images != null && !images.isEmpty()) {
                imageUrls = cloudinaryService.uploadImages(images);
            }
        } catch (java.io.IOException e) {
            throw new CustomException(500, "Internal Server Error", "UPLOAD_FAILED", "Failed to upload images");
        }

        Report report = Report.builder()
                .type(ReportType.valueOf(request.getType().toUpperCase().replace("-", "_")))
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ReportStatus.MISSING)
                .reward(request.getReward() != null && request.getReward() > 0 ? request.getReward() : null)
                .contact(request.getContact())
                .images(imageUrls)
                .location(request.getLocation())
                .details(request.getDetails())
                .lastSeen(request.getLastSeen())
                .reporter(reporter)
                .build();

        report = reportRepository.save(report);
        
        // Notify subscribers asynchronously via 20KM Haversine geofence
        boolean isCityWide = request.getIsCityWide() != null ? request.getIsCityWide() : false;
        try {
            subscriptionService.notifySubscribers(report, isCityWide);
        } catch (Exception e) {
            log.error("Failed to trigger subscriber notifications for report {}", report.getId(), e);
        }

        // Trigger future AI processing pipeline hook
        try {
            aiProcessingService.processNewReport(report);
        } catch (Exception e) {
            log.warn("AI processing hook skipped: {}", e.getMessage());
        }
        
        return reportMapper.reportToReportDto(report);
    }

    /**
     * Updates editable fields of an existing report, enforcing strict author ownership checks.
     *
     * @param userId Authenticated user ID.
     * @param reportId Report identifier to update.
     * @param request Updated field payload.
     * @return Updated {@link ReportDto}.
     * @throws CustomException with HTTP 403 if the user is not the owner.
     */
    @Transactional
    public ReportDto updateReport(String userId, String reportId, CreateReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Report not found"));

        if (!report.getReporter().getId().equals(userId)) {
            throw new CustomException(403, "Forbidden", "NOT_OWNER", "You do not own this report");
        }

        // We only allow updating certain fields (type and reporter are immutable)
        if (request.getTitle() != null) report.setTitle(request.getTitle());
        if (request.getDescription() != null) report.setDescription(request.getDescription());
        if (request.getReward() != null) report.setReward(request.getReward() > 0 ? request.getReward() : null);
        if (request.getContact() != null) report.setContact(request.getContact());
        if (request.getImages() != null) report.setImages(request.getImages());
        if (request.getLocation() != null) report.setLocation(request.getLocation());
        if (request.getDetails() != null) report.setDetails(request.getDetails());
        if (request.getLastSeen() != null) report.setLastSeen(request.getLastSeen());

        report = reportRepository.save(report);
        return reportMapper.reportToReportDto(report);
    }

    /**
     * Updates the status of a report (MISSING -> FOUND -> RESOLVED) and dispatches status notifications.
     *
     * @param userId Authenticated user ID.
     * @param reportId Report identifier.
     * @param request Target status payload.
     * @return Updated {@link ReportDto}.
     */
    @Transactional
    public ReportDto updateReportStatus(String userId, String reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Report not found"));

        if (!report.getReporter().getId().equals(userId)) {
            throw new CustomException(403, "Forbidden", "NOT_OWNER", "You do not own this report");
        }

        ReportStatus newStatus = ReportStatus.valueOf(request.getStatus().toUpperCase());
        report.setStatus(newStatus);
        report = reportRepository.save(report);

        // Create in-app notification for reporter confirming status update
        try {
            notificationService.createNotification(
                    report.getReporter(),
                    "REPORT_STATUS_CHANGED",
                    "Status updated: " + report.getTitle(),
                    "Your report has been marked as " + newStatus.name(),
                    report.getId()
            );
        } catch (Exception e) {
            log.warn("Failed to notify reporter of status update: {}", e.getMessage());
        }

        // Notify nearby subscribers of status update
        try {
            subscriptionService.notifySubscribers(report, false);
        } catch (Exception e) {
            log.warn("Failed to notify subscribers of status update: {}", e.getMessage());
        }

        return reportMapper.reportToReportDto(report);
    }

    /**
     * Deletes a report, pruning Cloudinary images, cascading notifications, and removing DB records.
     *
     * @param userId Authenticated user ID.
     * @param reportId Target report identifier.
     */
    @Transactional
    public void deleteReport(String userId, String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(404, "Not Found", "NOT_FOUND", "Report not found"));

        if (!report.getReporter().getId().equals(userId)) {
            throw new CustomException(403, "Forbidden", "NOT_OWNER", "You do not own this report");
        }

        // 1. Delete associated images from Cloudinary
        cloudinaryService.deleteImages(report.getImages());
        
        // 2. Delete any notifications related to this report
        notificationService.deleteNotificationsByReportId(report.getId());

        // 3. Delete report from database
        reportRepository.delete(report);
    }

    /**
     * Fetches all reports authored by a specific user with pagination.
     *
     * @param userId The author user ID.
     * @param page Zero-indexed page.
     * @param size Slice size.
     * @return {@link PaginatedResponse} containing authored reports.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ReportDto> getMyReports(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reportPage = reportRepository.findByReporterId(userId, pageable);
        return PaginatedResponse.of(reportPage.map(reportMapper::reportToReportDto));
    }

    /**
     * Computes real-time platform statistics including total cases, active, found, resolved, and counts by category.
     *
     * @return Map containing calculated aggregate counts.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long total = reportRepository.count();
        long missing = reportRepository.countByStatus(ReportStatus.MISSING);
        long found = reportRepository.countByStatus(ReportStatus.FOUND);
        long resolved = reportRepository.countByStatus(ReportStatus.RESOLVED);

        Map<String, Long> byType = new HashMap<>();
        byType.put("person", reportRepository.countByType(ReportType.PERSON));
        byType.put("pet", reportRepository.countByType(ReportType.PET));
        byType.put("lost-vehicle", reportRepository.countByType(ReportType.LOST_VEHICLE));
        byType.put("stolen-vehicle", reportRepository.countByType(ReportType.STOLEN_VEHICLE));

        return Map.of(
                "total", total,
                "active", missing,
                "found", found,
                "resolved", resolved,
                "byType", byType
        );
    }

    /**
     * Retrieves geocoded reports for display on the interactive Leaflet map.
     */
    public PaginatedResponse<ReportDto> getMapReports(String q, String type, String status, String city,
                                                   Double lat, Double lng, Double radiusKm) {
        return getReports(q, type, status, city, lat, lng, radiusKm, "newest", 0, 500);
    }
}
