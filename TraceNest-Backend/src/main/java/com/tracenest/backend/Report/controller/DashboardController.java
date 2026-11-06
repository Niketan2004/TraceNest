package com.tracenest.backend.Report.controller;

import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Report.dto.DashboardSummaryDto;
import com.tracenest.backend.Report.dto.ReportDto;
import com.tracenest.backend.Report.mapper.ReportMapper;
import com.tracenest.backend.Report.model.ReportStatus;
import com.tracenest.backend.Report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        long totalCommunity = reportRepository.count();
        long activeCommunity = reportRepository.countByStatus(ReportStatus.MISSING);
        long foundCommunity = reportRepository.countByStatus(ReportStatus.FOUND);
        long resolvedCommunity = reportRepository.countByStatus(ReportStatus.RESOLVED);

        List<ReportDto> recent = reportRepository.findTop5ByReporterIdOrderByCreatedAtDesc(userDetails.getId())
                .stream().map(reportMapper::reportToReportDto).collect(Collectors.toList());

        long myTotal = reportRepository.countByReporterId(userDetails.getId());
        long myActive = reportRepository.countByReporterIdAndStatus(userDetails.getId(), ReportStatus.MISSING);
        long myFound = reportRepository.countByReporterIdAndStatus(userDetails.getId(), ReportStatus.FOUND);
        long myResolved = reportRepository.countByReporterIdAndStatus(userDetails.getId(), ReportStatus.RESOLVED);

        DashboardSummaryDto response = DashboardSummaryDto.builder()
                .myReports(DashboardSummaryDto.Stats.builder()
                        .total(myTotal).active(myActive).found(myFound).resolved(myResolved).build())
                .communityStats(DashboardSummaryDto.Stats.builder()
                        .total(totalCommunity).active(activeCommunity).found(foundCommunity).resolved(resolvedCommunity).build())
                .recent(recent)
                .build();

        return ResponseEntity.ok(response);
    }
}
