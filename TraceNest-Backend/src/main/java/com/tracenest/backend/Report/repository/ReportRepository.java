package com.tracenest.backend.Report.repository;

import com.tracenest.backend.Report.model.Report;
import com.tracenest.backend.Report.model.ReportStatus;
import com.tracenest.backend.Report.model.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String>, JpaSpecificationExecutor<Report> {
    Page<Report> findByReporterId(String reporterId, Pageable pageable);
    List<Report> findTop5ByReporterIdOrderByCreatedAtDesc(String reporterId);
    
    long countByStatus(ReportStatus status);
    long countByType(ReportType type);
    long countByReporterId(String reporterId);
    long countByReporterIdAndStatus(String reporterId, ReportStatus status);
}
