package com.example.iter.delivery.domain.repository;

import com.example.iter.delivery.domain.entity.Shipping;
import com.example.iter.delivery.domain.entity.ShippingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingRepository extends JpaRepository<Shipping, Long> {
    List<Shipping> findByRentalId(Long rentalId);

    Optional<Shipping> findByRentalIdAndType(Long rentalId, ShippingType type);
}
