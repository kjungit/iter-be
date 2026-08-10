package com.example.iter.dispute.domain.repository;

import com.example.iter.dispute.domain.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}
