package com.tracenest.backend.Report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReportStatusRequest {
    @NotNull
    private String status;
}
