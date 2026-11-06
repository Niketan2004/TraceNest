package com.tracenest.backend.Report.service;

import com.tracenest.backend.Common.dto.PaginatedResponse;
import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Notification.service.NotificationService;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PosterService posterService;
    @Mock
    private AiProcessingService aiProcessingService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void testGetReportById_Success() {
        Report report = Report.builder().id("r-1").title("Missing Pet").build();
        when(reportRepository.findById("r-1")).thenReturn(Optional.of(report));
        when(reportMapper.reportToReportDto(report)).thenReturn(ReportDto.builder().id("r-1").title("Missing Pet").build());

        ReportDto dto = reportService.getReportById("r-1");

        assertNotNull(dto);
        assertEquals("r-1", dto.getId());
    }

    @Test
    void testGetReportById_NotFoundThrows() {
        when(reportRepository.findById("non-existent")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> reportService.getReportById("non-existent"));
        assertEquals("NOT_FOUND", ex.getCode());
    }

    @Test
    void testUpdateReportStatus_Success() {
        User reporter = User.builder().id("u1").name("Reporter").build();
        Report report = Report.builder()
                .id("r-1")
                .title("Case 1")
                .status(ReportStatus.MISSING)
                .reporter(reporter)
                .build();

        when(reportRepository.findById("r-1")).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenReturn(report);
        when(reportMapper.reportToReportDto(report)).thenReturn(ReportDto.builder().id("r-1").status(ReportStatus.FOUND).build());

        UpdateReportStatusRequest req = new UpdateReportStatusRequest();
        req.setStatus("FOUND");

        ReportDto res = reportService.updateReportStatus("u1", "r-1", req);

        assertEquals(ReportStatus.FOUND, report.getStatus());
        verify(notificationService).createNotification(eq(reporter), eq("REPORT_STATUS_CHANGED"), anyString(), anyString(), eq("r-1"));
        verify(subscriptionService).notifySubscribers(report, false);
    }

    @Test
    void testUpdateReportStatus_NonOwnerThrowsForbidden() {
        User reporter = User.builder().id("u1").build();
        Report report = Report.builder().id("r-1").reporter(reporter).build();

        when(reportRepository.findById("r-1")).thenReturn(Optional.of(report));

        UpdateReportStatusRequest req = new UpdateReportStatusRequest();
        req.setStatus("RESOLVED");

        CustomException ex = assertThrows(CustomException.class, () -> reportService.updateReportStatus("other-user", "r-1", req));
        assertEquals("NOT_OWNER", ex.getCode());
    }

    @Test
    void testGetStats_ReturnsCorrectCounts() {
        when(reportRepository.count()).thenReturn(100L);
        when(reportRepository.countByStatus(ReportStatus.MISSING)).thenReturn(60L);
        when(reportRepository.countByStatus(ReportStatus.FOUND)).thenReturn(25L);
        when(reportRepository.countByStatus(ReportStatus.RESOLVED)).thenReturn(15L);
        when(reportRepository.countByType(ReportType.PERSON)).thenReturn(20L);
        when(reportRepository.countByType(ReportType.PET)).thenReturn(40L);
        when(reportRepository.countByType(ReportType.LOST_VEHICLE)).thenReturn(20L);
        when(reportRepository.countByType(ReportType.STOLEN_VEHICLE)).thenReturn(20L);

        Map<String, Object> stats = reportService.getStats();

        assertEquals(100L, stats.get("total"));
        assertEquals(60L, stats.get("active"));
        assertEquals(25L, stats.get("found"));
        assertEquals(15L, stats.get("resolved"));
        assertNotNull(stats.get("byType"));
    }
}
