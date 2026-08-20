package com.example.iter.device.service;

import com.example.iter.device.storage.EquipmentImageStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EquipmentImageCleanupServiceTest {

    private final EquipmentImageStorage imageStorage = mock(EquipmentImageStorage.class);
    private final EquipmentImageCleanupService cleanupService =
            new EquipmentImageCleanupService(imageStorage);

    @Test
    void 일부_객체_삭제가_실패해도_나머지_객체를_계속_정리한다() {
        doThrow(new RuntimeException("S3 failure"))
                .when(imageStorage).delete("equipment/temp/1/first.jpg");

        cleanupService.deleteAll(
                List.of("equipment/temp/1/first.jpg", "equipment/temp/1/second.jpg"),
                "장비 이미지 정리 실패"
        );

        verify(imageStorage).delete("equipment/temp/1/first.jpg");
        verify(imageStorage).delete("equipment/temp/1/second.jpg");
    }
}
