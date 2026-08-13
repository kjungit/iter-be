package com.example.iter.auth.util;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminUserMapper {

     // 회원 정보와 거래·연체·신고 집계값을 관리자 회원 상세 응답으로 변환합니다.
    public AdminUserDetailResponse toDetail(
            User user,
            long rentedCount,
            long lentCount,
            long overdueCount,
            long reportCount
    ) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                rentedCount,
                lentCount,
                overdueCount,
                reportCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
