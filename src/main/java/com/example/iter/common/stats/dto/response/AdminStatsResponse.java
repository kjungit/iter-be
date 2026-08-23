package com.example.iter.common.stats.dto.response;

public record AdminStatsResponse(
        long userCount,
        long equipmentCount,
        long unresolvedReportCount,
        long paymentCount
) {
    public static AdminStatsResponse of(
            long userCount,
            long equipmentCount,
            long unresolvedReportCount,
            long paymentCount
    ) {
        return new AdminStatsResponse(userCount, equipmentCount, unresolvedReportCount, paymentCount);
    }
}
