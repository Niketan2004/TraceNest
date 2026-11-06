package com.tracenest.backend.Report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tracenest.backend.Report.model.Location;
import com.tracenest.backend.Report.model.ReportDetails;
import com.tracenest.backend.Report.model.ReportStatus;
import com.tracenest.backend.Report.model.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDto {
    private String id;
    private ReportType type;
    private String title;
    private String description;
    private ReportStatus status;
    private Integer reward;
    private String contact;
    private List<String> images;
    
    private Location location;
    private ReportDetails details;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastSeen;

    private ReporterDto reporter;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReporterDto {
        private String id;
        private String name;
        private String avatarUrl;
    }
}
