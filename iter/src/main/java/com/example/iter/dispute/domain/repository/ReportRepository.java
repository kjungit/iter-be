package com.example.iter.dispute.domain.repository;

import com.example.iter.dispute.domain.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
