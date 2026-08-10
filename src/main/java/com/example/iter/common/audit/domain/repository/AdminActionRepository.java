package com.example.iter.common.audit.domain.repository;

import com.example.iter.common.audit.domain.entity.AdminAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {
}
