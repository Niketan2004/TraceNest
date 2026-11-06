package com.tracenest.backend.Upload.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        String publicId = "report_" + UUID.randomUUID().toString();
        
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "traceNet",
                "public_id", publicId
        ));
        
        return uploadResult.get("secure_url").toString();
    }

    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    urls.add(uploadImage(file));
                }
            }
        }
        return urls;
    }

    public void deleteImage(String url) {
        try {
            int folderIndex = url.indexOf("traceNet/");
            if (folderIndex != -1) {
                String publicIdWithExt = url.substring(folderIndex);
                int extIndex = publicIdWithExt.lastIndexOf('.');
                String publicId = extIndex != -1 ? publicIdWithExt.substring(0, extIndex) : publicIdWithExt;
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            System.err.println("Failed to delete image from Cloudinary: " + url);
        }
    }

    public void deleteImages(List<String> urls) {
        if (urls != null) {
            for (String url : urls) {
                deleteImage(url);
            }
        }
    }
}
