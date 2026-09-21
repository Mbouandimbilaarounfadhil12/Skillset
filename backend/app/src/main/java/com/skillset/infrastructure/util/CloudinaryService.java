package com.skillset.infrastructure.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}")    String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {

        if (!cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank()) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key",    apiKey,
                    "api_secret", apiSecret,
                    "secure",     true
            ));
            log.info("Cloudinary configuré pour le cloud '{}'", cloudName);
        } else {
            this.cloudinary = null;
            log.warn("Cloudinary non configuré — les CV ne seront pas stockés dans le cloud.");
        }
    }

    /**
     * Upload un CV PDF sur Cloudinary.
     * Retourne l'URL sécurisée, ou null si Cloudinary n'est pas configuré.
     */
    public String uploadCv(MultipartFile file) {
        if (cloudinary == null) return null;

        try {
            String publicId = "skillset/cvs/" + UUID.randomUUID();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id",     publicId,
                            "overwrite",     false
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Échec de l'envoi du CV sur Cloudinary : " + e.getMessage());
        }
    }

    /**
     * Upload une photo de profil (image) sur Cloudinary.
     * Retourne l'URL sécurisée, ou null si Cloudinary n'est pas configuré.
     */
    public String uploadImage(MultipartFile file) {
        if (cloudinary == null) return null;

        try {
            String publicId = "skillset/avatars/" + UUID.randomUUID();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "public_id",     publicId,
                            "overwrite",     true
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Échec de l'envoi de la photo sur Cloudinary : " + e.getMessage());
        }
    }

    /**
     * Upload un PDF généré côté serveur (bytes bruts, pas de MultipartFile) sur Cloudinary.
     * Retourne l'URL sécurisée, ou null si Cloudinary n'est pas configuré ou en cas d'échec.
     */
    public String uploadPdfBytes(byte[] pdfBytes, String publicId) {
        if (cloudinary == null) return null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    pdfBytes,
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id",     publicId,
                            "overwrite",     true
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Échec de l'envoi du CV généré sur Cloudinary : {}", e.getMessage());
            return null;
        }
    }
}
