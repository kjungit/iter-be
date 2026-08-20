package com.example.iter.device.service;

import com.example.iter.common.config.AsyncConfig;
import com.example.iter.device.storage.EquipmentImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentImageCleanupService {

    private final EquipmentImageStorage imageStorage;

    @Async(AsyncConfig.S3_TASK_EXECUTOR)
    public void deleteAll(List<String> objectKeys, String failureMessage) {
        objectKeys.stream()
                .filter(objectKey -> objectKey != null && !objectKey.isBlank())
                .forEach(objectKey -> deleteQuietly(objectKey, failureMessage));
    }

    private void deleteQuietly(String objectKey, String failureMessage) {
        try {
            imageStorage.delete(objectKey);
        } catch (RuntimeException exception) {
            log.error("{}: objectKey={}", failureMessage, objectKey, exception);
        }
    }
}
