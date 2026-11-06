package com.tracenest.backend.Upload.controller;

import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Upload.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller managing media uploads to Cloudinary CDN.
 * <p>
 * Enforces strict client payload validation:
 * <ul>
 *   <li>Maximum 5 concurrent image files per request</li>
 *   <li>Maximum 5 MB size cap per file</li>
 *   <li>MIME type restriction (JPEG, PNG, WebP, GIF)</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/uploads/images")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final CloudinaryService cloudinaryService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    /**
     * Uploads a batch of validated image files to Cloudinary and returns their CDN URLs.
     *
     * @param files List of multipart image files.
     * @return {@link ResponseEntity} with HTTP 201 Created and list of upload results.
     */
    @PostMapping
    public ResponseEntity<Map<String, List<Map<String, Object>>>> uploadImages(
            @RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new CustomException(400, "Bad Request", "NO_FILES", "At least one file is required");
        }
        if (files.size() > 5) {
            throw new CustomException(400, "Bad Request", "TOO_MANY_FILES", "Maximum 5 files allowed");
        }

        // Validate all files first
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new CustomException(400, "Bad Request", "EMPTY_FILE", "File is empty: " + file.getOriginalFilename());
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new CustomException(400, "Bad Request", "FILE_TOO_LARGE", "File exceeds 5MB: " + file.getOriginalFilename());
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new CustomException(400, "Bad Request", "INVALID_TYPE", "Invalid file type: " + contentType);
            }
        }

        List<Map<String, Object>> responseFiles = new ArrayList<>();

        try {
            List<String> urls = cloudinaryService.uploadImages(files);
            for (int i = 0; i < urls.size(); i++) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("url", urls.get(i));
                fileInfo.put("sizeBytes", files.get(i).getSize());
                responseFiles.add(fileInfo);
            }
        } catch (IOException e) {
            log.error("Failed to upload images to Cloudinary", e);
            throw new CustomException(500, "Internal Server Error", "UPLOAD_FAILED", "Failed to upload images");
        }

        return new ResponseEntity<>(Map.of("files", responseFiles), HttpStatus.CREATED);
    }
}
