package com.tracenest.backend.Report.service;

import com.tracenest.backend.Report.model.Location;
import com.tracenest.backend.Report.model.Report;
import com.tracenest.backend.Report.model.ReportDetails;
import com.tracenest.backend.Report.model.ReportStatus;
import com.tracenest.backend.Report.model.ReportType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PosterServiceTest {

    private final PosterService posterService = new PosterService();

    @Test
    void testGeneratePoster_Success() {
        Report report = Report.builder()
                .id("test-report-123")
                .type(ReportType.PET)
                .title("Lost Golden Retriever")
                .description("Friendly dog wearing red collar, last seen near Cubbon Park.")
                .status(ReportStatus.MISSING)
                .reward(5000)
                .contact("+91 9876543210")
                .images(List.of())
                .location(Location.builder()
                        .address("MG Road")
                        .city("Bengaluru")
                        .state("Karnataka")
                        .build())
                .details(ReportDetails.builder()
                        .species("Dog")
                        .breed("Golden Retriever")
                        .color("Golden")
                        .build())
                .lastSeen(OffsetDateTime.now().minusHours(24))
                .build();

        byte[] pdfBytes = posterService.generatePoster(report);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF size should be at least 500 bytes");

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(8, pdfBytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "Output bytes should start with %PDF header");
    }

    @Test
    void testGeneratePoster_VehicleReport() {
        Report report = Report.builder()
                .id("veh-456")
                .type(ReportType.STOLEN_VEHICLE)
                .title("Stolen Royal Enfield Hunter")
                .description("Black Royal Enfield Hunter 350, small dent on tank.")
                .status(ReportStatus.MISSING)
                .contact("+91 9123456780")
                .images(List.of())
                .location(Location.builder().city("Pune").build())
                .details(ReportDetails.builder()
                        .vehicleNumber("MH 12 AB 1234")
                        .manufacturer("Royal Enfield")
                        .model("Hunter 350")
                        .firNumber("FIR-2026-99")
                        .build())
                .build();

        byte[] pdfBytes = posterService.generatePoster(report);

        assertNotNull(pdfBytes);
        String header = new String(pdfBytes, 0, 8, StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"));
    }
}
