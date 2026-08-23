package com.example.iter.payment.domain.repository;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.service.model.AdminPaymentDetailRow;
import com.example.iter.payment.service.model.AdminPaymentSummaryRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminPaymentQueryRepository extends Repository<Payment, Long> {

    // 검색어가 없을 때 결제 목록에 필요한 행만 조인해 커서 다음 위치부터 조회합니다.
    @Query("""
                    select new com.example.iter.payment.service.model.AdminPaymentSummaryRow(
                        p.id, p.rentalId, p.orderId, r.renterId,
                        u.email, u.name, u.nickname,
                        r.productNameSnapshot,
                        p.amount,
                        p.status, r.status,
                        p.paidAt, p.refundedAt, p.createdAt
                    )
                    from Payment p, Rental r, User u
                    where p.rentalId = r.id
                      and r.renterId = u.id
                      and (:status is null or p.status = :status)
                      and (:fromDateTime is null or p.createdAt >= :fromDateTime)
                      and (:toDateTimeExclusive is null or p.createdAt < :toDateTimeExclusive)
                      and (
                            :cursorCreatedAt is null
                            or p.createdAt < :cursorCreatedAt
                            or (p.createdAt = :cursorCreatedAt and p.id < :cursorId)
                          )
                    order by p.createdAt desc, p.id desc
                    """)
    List<AdminPaymentSummaryRow> searchWithoutKeywordForAdminByCursor(
            @Param("status") PaymentStatus status,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 결제, 대여, 대여자를 조인해 관리자 결제 목록에 필요한 데이터를 한 번에 조회합니다.
    // LOCATE를 사용하여 %, _ 등의 문자를 와일드카드가 아닌 실제 검색 문자로 처리합니다.
    @Query("""
                    select new com.example.iter.payment.service.model.AdminPaymentSummaryRow(
                        p.id, p.rentalId, p.orderId, r.renterId,
                        u.email, u.name, u.nickname,
                        r.productNameSnapshot,
                        p.amount,
                        p.status, r.status,
                        p.paidAt, p.refundedAt, p.createdAt
                    )
                    from Payment p, Rental r, User u
                    where p.rentalId = r.id
                      and r.renterId = u.id
                      and (
                            :keyword is null
                            or locate(lower(:keyword), lower(coalesce(p.orderId, ''))) > 0
                            or locate(lower(:keyword), lower(r.productNameSnapshot)) > 0
                            or locate(lower(:keyword), lower(u.email)) > 0
                            or locate(lower(:keyword), lower(u.name)) > 0
                            or locate(lower(:keyword), lower(coalesce(u.nickname, ''))) > 0
                          )
                      and (:status is null or p.status = :status)
                      and (:fromDateTime is null or p.createdAt >= :fromDateTime)
                      and (:toDateTimeExclusive is null or p.createdAt < :toDateTimeExclusive)
                      and (
                            :cursorCreatedAt is null
                            or p.createdAt < :cursorCreatedAt
                            or (p.createdAt = :cursorCreatedAt and p.id < :cursorId)
                          )
                    order by p.createdAt desc, p.id desc
                    """)
    List<AdminPaymentSummaryRow> searchForAdminByCursor(
            @Param("keyword") String keyword,
            @Param("status") PaymentStatus status,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 결제 상세 응답에 필요한 결제, 대여, 대여자 필드만 한 번에 조회합니다.
    @Query("""
            select new com.example.iter.payment.service.model.AdminPaymentDetailRow(
                p.id, p.rentalId, p.orderId, r.renterId,
                u.email, u.name, u.nickname,
                r.equipmentId, r.productNameSnapshot, r.categorySnapshot, r.dailyPriceSnapshot, r.startDate, r.endDate, r.rentalDays, r.totalPrice,
                p.amount,
                p.status, r.status,
                p.paidAt, p.refundedAt, p.createdAt, p.updatedAt
            )
            from Payment p, Rental r, User u
            where p.id = :paymentId
              and p.rentalId = r.id
              and r.renterId = u.id
            """)
    Optional<AdminPaymentDetailRow> findDetailById(@Param("paymentId") Long paymentId);
}
