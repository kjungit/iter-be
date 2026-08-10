package com.example.iter.delivery.domain.repository;

import com.example.iter.delivery.domain.entity.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingRepository extends JpaRepository<Shipping, Long> {
    List<Shipping> findByRentalId(Long rentalId);
}
