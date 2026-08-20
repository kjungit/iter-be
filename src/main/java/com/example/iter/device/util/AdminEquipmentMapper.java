package com.example.iter.device.util;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.dto.response.AdminEquipmentDetailResponse;
import com.example.iter.device.dto.response.AdminEquipmentSummaryResponse;
import com.example.iter.device.dto.response.ImageResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminEquipmentMapper {

    // 장비, 등록자, 썸네일을 관리자 장비 목록 응답으로 변환합니다.
    public AdminEquipmentSummaryResponse toSummary(
            Equipment equipment,
            User owner,
            String thumbnailUrl
    ) {
        return new AdminEquipmentSummaryResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getDailyPrice(),
                equipment.getStatus(),
                UserSummaryResponse.from(owner),
                thumbnailUrl,
                equipment.getCreatedAt()
        );
    }

    // 장비, 등록자, 전체 이미지 목록을 관리자 장비 상세 응답으로 변환합니다.
    public AdminEquipmentDetailResponse toDetail(
            Equipment equipment,
            User owner,
            List<EquipmentImage> images
    ) {
        List<ImageResponse> imageResponses = images.stream().map(this::toImageResponse).toList();

        return new AdminEquipmentDetailResponse(
                equipment.getId(),
                UserSummaryResponse.from(owner),
                equipment.getCategory(),
                equipment.getName(),
                equipment.getDescription(),
                equipment.getDailyPrice(),
                equipment.getAvailableFrom(),
                equipment.getAvailableTo(),
                equipment.getStatus(),
                equipment.getProductCondition(),
                equipment.getConditionDetail(),
                imageResponses,
                equipment.getCreatedAt(),
                equipment.getUpdatedAt()
        );
    }

    // 장비 이미지 엔티티를 이미지 응답 DTO로 변환합니다.
    private ImageResponse toImageResponse(EquipmentImage image) {
        return new ImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getSortOrder(),
                image.isThumbnail()
        );
    }
}
