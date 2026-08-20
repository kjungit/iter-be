package com.example.iter.device.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EquipmentImagePolicy {

    public static final int MIN_IMAGE_COUNT = 1;
    public static final int MAX_IMAGE_COUNT = 5;
    public static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public void validateMetadata(String contentType, long size) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)
                || size <= 0
                || size > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    public void validateSignature(String contentType, byte[] header) {
        boolean valid = switch (contentType == null ? "" : contentType) {
            case "image/jpeg" -> isJpeg(header);
            case "image/png" -> isPng(header);
            case "image/webp" -> isWebp(header);
            default -> false;
        };
        if (!valid) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    public String extensionOf(String contentType) {
        return switch (contentType == null ? "" : contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new CustomException(ErrorCode.INVALID_IMAGE);
        };
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (header.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(header[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }
}
