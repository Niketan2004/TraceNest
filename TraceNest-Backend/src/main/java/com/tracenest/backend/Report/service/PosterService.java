package com.tracenest.backend.Report.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.tracenest.backend.Report.model.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;

/**
 * High-performance document generation service utilizing OpenPDF (LibrePDF).
 * <p>
 * Dynamically synthesizes printable A4 emergency flyers and search posters
 * containing:
 * <ul>
 *   <li>High-visibility alert banners color-coded by incident category.</li>
 *   <li>Primary report photography fetched dynamically from Cloudinary CDN.</li>
 *   <li>Key identifying attributes (age, clothing, vehicle registration, pet breed).</li>
 *   <li>Reward callout boxes and emergency contact information.</li>
 *   <li>TraceNest verification footers with incident tracking IDs.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class PosterService {

    /**
     * Generates a complete A4 PDF flyer for a given incident report.
     *
     * @param report Domain entity containing report information, location, and images.
     * @return Rendered PDF document as byte array.
     * @throws RuntimeException if PDF construction fails.
     */
    public byte[] generatePoster(Report report) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color primaryRed = new Color(220, 38, 38);
            Color darkSlate = new Color(15, 23, 42);
            Color textGray = new Color(71, 85, 105);
            Color lightBg = new Color(248, 250, 252);
            Color amberBg = new Color(254, 243, 199);
            Color amberBorder = new Color(217, 119, 6);

            // Fonts
            Font bannerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Color.WHITE);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, darkSlate);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryRed);
            Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, darkSlate);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkSlate);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textGray);
            Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkSlate);
            Font rewardFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, amberBorder);
            Font contactFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textGray);

            // 1. TOP BANNER
            String bannerText = "MISSING";
            if (report.getType() != null && report.getType().name().contains("VEHICLE")) {
                bannerText = report.getType().name().contains("STOLEN") ? "STOLEN VEHICLE" : "LOST VEHICLE";
            } else if (report.getType() != null && report.getType().name().equals("PET")) {
                bannerText = "LOST PET";
            }

            PdfPTable bannerTable = new PdfPTable(1);
            bannerTable.setWidthPercentage(100);
            PdfPCell bannerCell = new PdfPCell(new Phrase(bannerText + " ALERT", bannerFont));
            bannerCell.setBackgroundColor(primaryRed);
            bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bannerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bannerCell.setPaddingTop(12);
            bannerCell.setPaddingBottom(12);
            bannerCell.setBorder(Rectangle.NO_BORDER);
            bannerTable.addCell(bannerCell);
            document.add(bannerTable);

            // Spacing
            document.add(new Paragraph(" "));

            // 2. TITLE & SUBTITLE
            Paragraph titlePara = new Paragraph(report.getTitle(), titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            String cityState = "";
            if (report.getLocation() != null) {
                if (report.getLocation().getCity() != null) cityState += report.getLocation().getCity();
                if (report.getLocation().getState() != null) cityState += ", " + report.getLocation().getState();
            }
            if (!cityState.isEmpty()) {
                Paragraph locPara = new Paragraph("LAST SEEN: " + cityState.toUpperCase(), subTitleFont);
                locPara.setAlignment(Element.ALIGN_CENTER);
                document.add(locPara);
            }

            document.add(new Paragraph(" "));

            // 3. IMAGE
            if (report.getImages() != null && !report.getImages().isEmpty()) {
                String firstImgUrl = report.getImages().get(0);
                try {
                    URI uri = URI.create(firstImgUrl);
                    try (InputStream in = uri.toURL().openStream()) {
                        byte[] imgBytes = in.readAllBytes();
                        Image img = Image.getInstance(imgBytes);
                        img.setAlignment(Element.ALIGN_CENTER);
                        img.scaleToFit(280, 240);
                        document.add(img);
                    }
                } catch (Exception e) {
                    log.warn("Could not download image for poster: {}", e.getMessage());
                }
            }

            document.add(new Paragraph(" "));

            // 4. KEY DETAILS GRID
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingBefore(10);
            detailsTable.setSpacingAfter(10);

            if (report.getDetails() != null) {
                var d = report.getDetails();
                if (d.getAge() != null) addDetailRow(detailsTable, "Age", d.getAge() + " years", labelFont, valueFont);
                if (d.getGender() != null) addDetailRow(detailsTable, "Gender", d.getGender(), labelFont, valueFont);
                if (d.getSpecies() != null) addDetailRow(detailsTable, "Species", d.getSpecies(), labelFont, valueFont);
                if (d.getBreed() != null) addDetailRow(detailsTable, "Breed", d.getBreed(), labelFont, valueFont);
                if (d.getColor() != null) addDetailRow(detailsTable, "Color / Markings", d.getColor(), labelFont, valueFont);
                if (d.getVehicleType() != null) addDetailRow(detailsTable, "Vehicle Type", d.getVehicleType(), labelFont, valueFont);
                if (d.getVehicleNumber() != null) addDetailRow(detailsTable, "License Plate", d.getVehicleNumber(), labelFont, valueFont);
                if (d.getManufacturer() != null) addDetailRow(detailsTable, "Make / Model", d.getManufacturer() + " " + (d.getModel() != null ? d.getModel() : ""), labelFont, valueFont);
                if (d.getFirNumber() != null) addDetailRow(detailsTable, "FIR Number", d.getFirNumber(), labelFont, valueFont);
                if (d.getPoliceStation() != null) addDetailRow(detailsTable, "Police Station", d.getPoliceStation(), labelFont, valueFont);
            }

            if (report.getLastSeen() != null) {
                String dateStr = report.getLastSeen().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                addDetailRow(detailsTable, "Date & Time Last Seen", dateStr, labelFont, valueFont);
            }

            if (detailsTable.size() > 0) {
                document.add(detailsTable);
            }

            // 5. DESCRIPTION
            if (report.getDescription() != null && !report.getDescription().isBlank()) {
                Paragraph descHeader = new Paragraph("Description & Circumstances:", sectionHeaderFont);
                document.add(descHeader);
                Paragraph descBody = new Paragraph(report.getDescription(), descFont);
                descBody.setSpacingAfter(10);
                document.add(descBody);
            }

            // 6. REWARD CALLOUT
            if (report.getReward() != null && report.getReward() > 0) {
                PdfPTable rewardTable = new PdfPTable(1);
                rewardTable.setWidthPercentage(100);
                PdfPCell rCell = new PdfPCell();
                rCell.setBackgroundColor(amberBg);
                rCell.setBorderColor(amberBorder);
                rCell.setPadding(8);
                Paragraph rPara = new Paragraph("REWARD OFFERED: ₹" + String.format("%,d", report.getReward()), rewardFont);
                rPara.setAlignment(Element.ALIGN_CENTER);
                rCell.addElement(rPara);
                rewardTable.addCell(rCell);
                document.add(rewardTable);
            }

            // 7. CONTACT ACTION BAR
            document.add(new Paragraph(" "));
            PdfPTable contactTable = new PdfPTable(1);
            contactTable.setWidthPercentage(100);
            PdfPCell contactCell = new PdfPCell();
            contactCell.setBackgroundColor(darkSlate);
            contactCell.setPadding(12);
            contactCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            contactCell.setBorder(Rectangle.NO_BORDER);

            Paragraph cHeader = new Paragraph("IF SEEN OR YOU HAVE INFORMATION, PLEASE CONTACT:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.LIGHT_GRAY));
            cHeader.setAlignment(Element.ALIGN_CENTER);
            contactCell.addElement(cHeader);

            String contactStr = report.getContact() != null ? report.getContact() : "TraceNest Platform";
            Paragraph cVal = new Paragraph(contactStr, contactFont);
            cVal.setAlignment(Element.ALIGN_CENTER);
            contactCell.addElement(cVal);

            contactTable.addCell(contactCell);
            document.add(contactTable);

            // 8. FOOTER
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph(
                    "TraceNest Community Safety Network | Report ID: " + report.getId() + " | Visit tracenest.app for live updates",
                    footerFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            log.error("Failed to build PDF poster", e);
            throw new RuntimeException("Error generating PDF poster", e);
        }

        return out.toByteArray();
    }

    private void addDetailRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        if (value == null || value.isBlank()) return;
        PdfPCell c1 = new PdfPCell(new Phrase(label + ":", labelFont));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(new Color(226, 232, 240));
        c1.setPadding(5);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(new Color(226, 232, 240));
        c2.setPadding(5);

        table.addCell(c1);
        table.addCell(c2);
    }
}
