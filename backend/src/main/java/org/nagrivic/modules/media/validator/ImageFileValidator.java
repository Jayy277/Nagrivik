package org.nagrivic.modules.media.validator;

import org.nagrivic.modules.media.config.MediaStorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Component
public class ImageFileValidator {

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF_HEADER = new byte[]{'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_HEADER = new byte[]{'W', 'E', 'B', 'P'};

    private final MediaStorageProperties properties;

    public ImageFileValidator(MediaStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates the uploaded file for emptiness, size, MIME type, extension, and magic bytes.
     * Returns the canonical extension (e.g., "jpg", "png", "webp") if valid.
     */
    public String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException(String.format(
                    "File size (%d bytes) exceeds the maximum allowed limit of %d MB",
                    file.getSize(),
                    properties.getMaxFileSizeMb()
            ));
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported media type: " + contentType + ". Supported types: JPEG, PNG, WebP");
        }
        contentType = contentType.toLowerCase(Locale.ROOT);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            validateExtensionMatchesContentType(originalFilename, contentType);
        }

        // Validate actual file magic bytes / signatures
        validateMagicBytes(file, contentType);

        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    private void validateExtensionMatchesContentType(String filename, String contentType) {
        String ext = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < filename.length() - 1) {
            ext = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }

        if (ext.isEmpty()) {
            return;
        }

        boolean matches = switch (contentType) {
            case "image/jpeg" -> ext.equals("jpg") || ext.equals("jpeg");
            case "image/png" -> ext.equals("png");
            case "image/webp" -> ext.equals("webp");
            default -> false;
        };

        if (!matches) {
            throw new IllegalArgumentException(String.format(
                    "File extension '%s' does not match declared Content-Type '%s'",
                    ext,
                    contentType
            ));
        }
    }

    private void validateMagicBytes(MultipartFile file, String declaredContentType) {
        byte[] header = new byte[12];
        int bytesRead;
        try (InputStream is = file.getInputStream()) {
            bytesRead = is.read(header);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file contents", e);
        }

        if (bytesRead < 3) {
            throw new IllegalArgumentException("Invalid file: content too small to be a valid image");
        }

        switch (declaredContentType) {
            case "image/jpeg" -> {
                if (!matchesPrefix(header, JPEG_MAGIC)) {
                    throw new IllegalArgumentException("File content does not match JPEG signature (corrupt or disguised file)");
                }
            }
            case "image/png" -> {
                if (bytesRead < 8 || !matchesPrefix(header, PNG_MAGIC)) {
                    throw new IllegalArgumentException("File content does not match PNG signature (corrupt or disguised file)");
                }
            }
            case "image/webp" -> {
                if (bytesRead < 12 || !isWebp(header)) {
                    throw new IllegalArgumentException("File content does not match WebP signature (corrupt or disguised file)");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported image format: " + declaredContentType);
        }
    }

    private boolean matchesPrefix(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] header) {
        // WebP has "RIFF" in bytes 0..3 and "WEBP" in bytes 8..11
        for (int i = 0; i < 4; i++) {
            if (header[i] != RIFF_HEADER[i]) {
                return false;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (header[8 + i] != WEBP_HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}
