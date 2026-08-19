package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.EquipmentImageUpload;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EquipmentImageUploadRepository
        extends JpaRepository<EquipmentImageUpload, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from EquipmentImageUpload u where u.objectKey in :objectKeys order by u.id")
    List<EquipmentImageUpload> findAllByObjectKeyInForUpdate(
            @Param("objectKeys") Collection<String> objectKeys
    );
}
