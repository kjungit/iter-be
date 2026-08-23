package com.example.iter.reservation.domain.repository.spec;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.springframework.data.jpa.domain.Specification;

// 대여자가 빌린 거래(findBorrowedHistory) 조회 조건을 조립한다.
// value/countQuery를 손으로 두 벌 유지하지 않기 위해 Specification으로 작성 (JpaSpecificationExecutor.findAll이
// 이 조건 하나로부터 목록 쿼리와 count 쿼리를 각각 생성해준다).
public class RentalSpecifications {

    private RentalSpecifications() {
    }

    public static Specification<Rental> renterIs(Long renterId) {
        return (root, query, cb) -> cb.equal(root.get("renterId"), renterId);
    }

    public static Specification<Rental> hasStatus(RentalStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    // 예약 당시 장비명(productNameSnapshot)에 검색어가 포함되는지(대소문자 무시) 확인한다.
    // LOCATE는 LIKE와 달리 검색어의 %, _를 와일드카드가 아닌 실제 문자로 취급하므로 그대로 사용한다
    // (LIKE로 바꾸면 검색어에 %/_가 포함될 때 의도와 다르게 동작한다).
    public static Specification<Rental> equipmentNameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null) {
                return null;
            }
            var locatePosition = cb.function(
                    "locate", Integer.class,
                    cb.literal(keyword.toLowerCase()),
                    cb.lower(root.get("productNameSnapshot"))
            );
            return cb.greaterThan(locatePosition, 0);
        };
    }

    public static Specification<Rental> borrowedHistory(Long renterId, RentalStatus status, String equipmentName) {
        return Specification.where(renterIs(renterId))
                .and(hasStatus(status))
                .and(equipmentNameContains(equipmentName));
    }
}
