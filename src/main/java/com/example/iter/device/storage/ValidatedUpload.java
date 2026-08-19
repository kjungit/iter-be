package com.example.iter.device.storage;

public record ValidatedUpload(
        String objectKey,
        String contentType,
        long size,
        String eTag
) {
}
