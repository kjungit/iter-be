package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    Optional<UserAddress> findByUserIdAndDefaultAddressTrue(Long userId);

    long countByUserIdAndDefaultAddressTrue(Long userId);
}
