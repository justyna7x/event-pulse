package com.eventpulse.api.repository;

import com.eventpulse.api.entity.CheckLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckLogRepository extends JpaRepository<CheckLog, Long> {
    List<CheckLog> findTop50ByEndpointIdOrderByCheckedAtDesc(Long endpointId);
}