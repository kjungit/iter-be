package com.example.iter.device.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

// ERD EQUIPMENT_IMAGE 엔티티 — Equipment와 같은 도메인이므로 정상적인 JPA 연관관계를 사용
@Entity
@Table(name = "equipment_image")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EquipmentImage extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "is_thumbnail")
    @Builder.Default
    private boolean thumbnail = false;

    public void changeThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
