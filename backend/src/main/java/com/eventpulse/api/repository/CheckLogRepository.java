package com.eventpulse.api.repository;

import com.eventpulse.api.entity.CheckLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckLogRepository extends JpaRepository<CheckLog, Long> {

    // Fetch recent check logs for a specific endpoint
    List<CheckLog> findByEndpointIdOrderByCheckedAtDesc(Long endpointId, Pageable pageable);
}