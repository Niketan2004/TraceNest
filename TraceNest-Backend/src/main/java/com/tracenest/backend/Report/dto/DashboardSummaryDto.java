package com.tracenest.backend.Report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private Stats myReports;
    private Stats communityStats;
    private List<ReportDto> recent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private long total;
        private long active;
        private long found;
        private long resolved;
    }
}
