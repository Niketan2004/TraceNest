package com.tracenest.backend.User.service;

import com.tracenest.backend.Common.exception.CustomException;
import com.tracenest.backend.Report.model.Report;
import com.tracenest.backend.Upload.service.CloudinaryService;
import com.tracenest.backend.User.dto.ChangePasswordRequest;
import com.tracenest.backend.User.dto.UpdateUserRequest;
import com.tracenest.backend.User.dto.UserDto;
import com.tracenest.backend.User.mapper.UserMapper;
import com.tracenest.backend.User.model.User;
import com.tracenest.backend.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public UserDto updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        
        return userMapper.userToUserDto(userRepository.save(user));
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(401, "Unauthorized", "INVALID_CREDENTIALS", "Incorrect current password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public String uploadAvatar(String userId, MultipartFile file) {
        User user = userRepository.findById(userId).orElseThrow();
        try {
            // Delete old avatar from Cloudinary if it exists
            if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("traceNet/")) {
                cloudinaryService.deleteImage(user.getAvatarUrl());
            }
            String url = cloudinaryService.uploadImage(file);
            user.setAvatarUrl(url);
            userRepository.save(user);
            return url;
        } catch (java.io.IOException e) {
            throw new CustomException(500, "Internal Server Error", "UPLOAD_FAILED", "Failed to upload avatar");
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // Ensure all Cloudinary images for all reports are destroyed
        // The DB cascade will handle the row deletions, but we must manually clean up the cloud
        if (user.getReports() != null) {
            for (Report report : user.getReports()) {
                if (report.getImages() != null && !report.getImages().isEmpty()) {
                    cloudinaryService.deleteImages(report.getImages());
                }
            }
        }
        
        // Also cleanup the avatar if we eventually start storing it in Cloudinary
        if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("traceNet/")) {
            cloudinaryService.deleteImage(user.getAvatarUrl());
        }

        userRepository.delete(user);
    }
}
