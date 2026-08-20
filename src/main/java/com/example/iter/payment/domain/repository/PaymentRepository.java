package com.example.iter.payment.domain.repository;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.service.model.PaymentHistoryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRentalId(Long rentalId);
    Optional<Payment> findByOrderId(String orderId);

    @Query(
            value = """
                    select new com.example.iter.payment.service.model.PaymentHistoryRow(
                        payment,
                        rental
                    )
                    from Payment payment
                    join Rental rental on rental.id = payment.rentalId
                    where rental.renterId = :userId
                      and (:status is null or payment.status = :status)
                    order by payment.createdAt desc, payment.id desc
                    """,
            countQuery = """
                    select count(payment.id)
                    from Payment payment
                    join Rental rental on rental.id = payment.rentalId
                    where rental.renterId = :userId
                      and (:status is null or payment.status = :status)
                    """
    )
    Page<PaymentHistoryRow> findMyPaymentHistory(
            @Param("userId") Long userId,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );
}
