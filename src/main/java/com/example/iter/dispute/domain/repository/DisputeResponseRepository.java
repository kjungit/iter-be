package com.example.iter.dispute.domain.repository;

import com.example.iter.dispute.domain.entity.DisputeResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeResponseRepository extends JpaRepository<DisputeResponse, Long> {
    List<DisputeResponse> findByDisputeIdOrderByCreatedAtAsc(Long disputeId);
}
