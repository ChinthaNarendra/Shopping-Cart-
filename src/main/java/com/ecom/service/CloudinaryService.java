package com.ecom.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file, String folder)
            throws IOException {

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select an image to upload.");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB.");
        }

        // Basic content-type validation
        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT)
                        .startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Invalid file type. Please upload an image.");
        }

        // Validate Cloudinary credentials
        Map<?, ?> config = cloudinary.config.asMap();

        if (isBlank(config.get("cloud_name"))
                || isBlank(config.get("api_key"))
                || isBlank(config.get("api_secret"))) {

            throw new IllegalStateException(
                    "Cloudinary credentials are missing. "
                    + "Please configure the environment variables.");
        }

        // Upload image to Cloudinary
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );

        // Return the permanent HTTPS image URL
        Object secureUrl = result.get("secure_url");

        if (secureUrl == null) {
            throw new IOException(
                    "Cloudinary did not return an image URL.");
        }

        return secureUrl.toString();
    }

    private boolean isBlank(Object value) {
        return value == null
                || value.toString().trim().isEmpty();
    }
}