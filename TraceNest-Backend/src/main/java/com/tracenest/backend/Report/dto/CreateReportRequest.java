package com.tracenest.backend.Report.dto;

import com.tracenest.backend.Report.model.Location;
import com.tracenest.backend.Report.model.ReportDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class CreateReportRequest {
    @NotNull
    private String type;
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String description;
    
    @NotBlank
    private String contact;
    
    private Integer reward;
    private OffsetDateTime lastSeen;
    
    @NotNull
    private Location location;
    
    private ReportDetails details;
    
    private List<String> images;
    
    private Boolean isCityWide;
}
